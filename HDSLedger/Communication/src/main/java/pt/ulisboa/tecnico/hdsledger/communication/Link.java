package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.Message.Type;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.security.interfaces.RSAPublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.Signature;
import javax.crypto.Cipher;


public class Link {

    private static final CustomLogger LOGGER = new CustomLogger(Link.class.getName());
    // Time to wait for an ACK before resending the message
    private final int BASE_SLEEP_TIME;
    // UDP Socket
    private final DatagramSocket socket;
    // Map of all nodes in the network
    private final Map<String, ProcessConfig> nodes = new ConcurrentHashMap<>();
    // Map of all clients in the network
    private final Map<String, ProcessConfig> clients = new ConcurrentHashMap<>();
    // Reference to the node itself
    private final ProcessConfig config;
    // Private Key Path
    private final String privateKey;
    // Class to deserialize messages to
    private final Class<? extends Message> messageClass;
    // Set of received messages from specific node (prevent duplicates)
    private final Map<String, CollapsingSet> receivedMessages = new ConcurrentHashMap<>();
    // Set of received ACKs from specific node
    private final CollapsingSet receivedAcks = new CollapsingSet();
    // Message counter
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    // Send messages to self by pushing to queue instead of through the network
    private final Queue<Message> localhostQueue = new ConcurrentLinkedQueue<>();

    public Link(ProcessConfig self, String privateKey ,int port, ProcessConfig[] nodes, Class<? extends Message> messageClass) {
        this(self, privateKey, port, nodes, messageClass, false, 200);
    }

    public Link(ProcessConfig self, String privateKey ,int port, ProcessConfig[] nodes, Class<? extends Message> messageClass,
            boolean activateLogs, int baseSleepTime) {

        this.config = self;
        this.privateKey = privateKey;
        this.messageClass = messageClass;
        this.BASE_SLEEP_TIME = baseSleepTime;

        Arrays.stream(nodes).forEach(node -> {
            String id = node.getId();
            if (id.contains("client")) {
                this.clients.put(id, node);
            }
            else {
                this.nodes.put(id, node);
            }
            receivedMessages.put(id, new CollapsingSet());
        });

        try {
            this.socket = new DatagramSocket(port, InetAddress.getByName(config.getHostname()));
        } catch (UnknownHostException | SocketException e) {
            throw new HDSSException(ErrorMessage.CannotOpenSocket);
        }
        if (!activateLogs) {
            LogManager.getLogManager().reset();
        }
    }

    public static PublicKey getPublicKey(String publicKeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(publicKeyPath));
            // remove the header, footer and newlines from key
            String uKey = new String(keyBytes);
            uKey = uKey.replace("-----BEGIN PUBLIC KEY-----", "");
            uKey = uKey.replace("-----END PUBLIC KEY-----", "");
            uKey = uKey.replaceAll("\\s+", "");
            keyBytes = Base64.getDecoder().decode(uKey);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
	}

    public static PrivateKey getPrivateKey(String privateKeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
            // remove the header, footer and newlines from key
            String rKey = new String(keyBytes);
            rKey = rKey.replace("-----BEGIN PRIVATE KEY-----", "");
            rKey = rKey.replace("-----END PRIVATE KEY-----", "");
            rKey = rKey.replaceAll("\\s+", "");
            keyBytes = Base64.getDecoder().decode(rKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
    }

    // Function that generates a key signature from a value
    public byte[] sign(byte[] data) {
        byte[] signature = new byte[256];
        // current node private key
        PrivateKey privKey = getPrivateKey(privateKey);
        try {
            // Create a Signature object and initialize it with the private key
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privKey);
            // Update and sign the data
            rsa.update(data);
            signature = rsa.sign();
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
        }
        return signature;
    }

    // inverse of the sign function aka validate
    public boolean validate(String nodeId, byte[] data, byte[] signature) {
        ProcessConfig sender = nodes.get(nodeId);
        if (sender == null)
            sender = clients.get(nodeId);
        // Other node public key
        String publicKey = sender.getPublicKey();
        //extract public key from .key file
        PublicKey pubKey = getPublicKey(publicKey);
        // validate signature
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(pubKey);
            rsa.update(data);
            return rsa.verify(signature);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
            return false;
        }
    }

    public void ackAll(List<Integer> messageIds) {
        receivedAcks.addAll(messageIds);
    }

    /*
     * Broadcasts a message to all nodes in the network
     *
     * @param data The message to be broadcasted
     */
    public void broadcast(Message data) {
        Gson gson = new Gson();
        nodes.forEach((destId, dest) -> send(destId, gson.fromJson(gson.toJson(data), data.getClass())));
    }

    /*
     * Sends a message to a specific node with guarantee of delivery
     *
     * @param nodeId The node identifier
     *
     * @param data The message to be sent
     */
    public void send(String nodeId, Message data) {

        // Spawn a new thread to send the message
        // To avoid blocking while waiting for ACK
        new Thread(() -> {
            try {
                ProcessConfig node = nodes.get(nodeId);
                if (node == null)
                    node = clients.get(nodeId);
                if (node == null)
                    throw new HDSSException(ErrorMessage.NoSuchNode);

                data.setMessageId(messageCounter.getAndIncrement());

                // If the message is not ACK, it will be resent
                InetAddress destAddress = InetAddress.getByName(node.getHostname());
                int destPort = node.getPort();
                int count = 1;
                int messageId = data.getMessageId();
                int sleepTime = BASE_SLEEP_TIME;

                // Send message to local queue instead of using network if destination in self
                if (nodeId.equals(this.config.getId())) {
                    this.localhostQueue.add(data);

                    LOGGER.log(Level.INFO,
                            MessageFormat.format("{0} - Message {1} (locally) sent to {2}:{3} successfully",
                                    config.getId(), data.getType(), destAddress, destPort));

                    return;
                }
                byte[] buf = new Gson().toJson(data).getBytes();
                byte[] signature = sign(buf);
                for (;;) {
                    LOGGER.log(Level.INFO, MessageFormat.format(
                            "{0} - Sending {1} message to {2}:{3} with message ID {4} - Attempt #{5}", config.getId(),
                            data.getType(), destAddress, destPort, messageId, count++));

                    unreliableSend(destAddress, destPort, data, signature);

                    // Wait (using exponential back-off), then look for ACK
                    Thread.sleep(sleepTime);

                    // Receive method will set receivedAcks when sees corresponding ACK
                    if (receivedAcks.contains(messageId))
                        break;

                    sleepTime <<= 1;
                }

                LOGGER.log(Level.INFO, MessageFormat.format("{0} - Message {1} sent to {2}:{3} successfully",
                        config.getId(), data.getType(), destAddress, destPort));
            } catch (InterruptedException | UnknownHostException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /*
     * Sends a message to a specific node without guarantee of delivery
     * Mainly used to send ACKs, if they are lost, the original message will be
     * resent
     *
     * @param address The address of the destination node
     *
     * @param port The port of the destination node
     *
     * @param data The message to be sent
     */
    public void unreliableSend(InetAddress hostname, int port, Message data, byte[] signature) {
        new Thread(() -> {
            try {
                byte[] buf = new Gson().toJson(data).getBytes();
                // Add signature to the message
                byte[] bufWithSignature = new byte[buf.length + signature.length];
                System.arraycopy(buf, 0, bufWithSignature, 0, buf.length);
                System.arraycopy(signature, 0, bufWithSignature, buf.length, signature.length);
                
                DatagramPacket packet = new DatagramPacket(bufWithSignature, bufWithSignature.length, hostname, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                throw new HDSSException(ErrorMessage.SocketSendingError);
            }
        }).start();
    }

    /*
     * Receives a message from any node in the network (blocking)
     */
    public Message receive() throws IOException, ClassNotFoundException {

        Message message = null;
        String serialized = "";
        Boolean local = false;
        DatagramPacket response = null;
        
        if (this.localhostQueue.size() > 0) {
            message = this.localhostQueue.poll();
            local = true; 
            this.receivedAcks.add(message.getMessageId());
        } else {
            // TODO: Check if buffer size is enough
            byte[] buf = new byte[65535];
            response = new DatagramPacket(buf, buf.length);
            
            socket.receive(response);

            byte[] buffer = Arrays.copyOfRange(response.getData(), 0, response.getLength());
            // split the buffer into the message and the signature
            byte[] messageBuffer = Arrays.copyOfRange(buffer, 0, buffer.length - 256);
            byte[] signature = Arrays.copyOfRange(buffer, buffer.length - 256, buffer.length);
            serialized = new String(messageBuffer);
            message = new Gson().fromJson(serialized, Message.class);
            // verify the signature
            if (!validate(message.getSenderId(), messageBuffer, signature)) {
                throw new HDSSException(ErrorMessage.InvalidSignature);
            }
        }

        String senderId = message.getSenderId();
        int messageId = message.getMessageId();

        if (!nodes.containsKey(senderId) && !clients.containsKey(senderId))
            throw new HDSSException(ErrorMessage.NoSuchNode);

        // Handle ACKS, since it's possible to receive multiple acks from the same
        // message
        if (message.getType().equals(Message.Type.ACK)) {
            receivedAcks.add(messageId);
            return message;
        }

        // It's not an ACK -> Deserialize for the correct type
        if (!local)
            message = new Gson().fromJson(serialized, this.messageClass);

        boolean isRepeated = !receivedMessages.get(message.getSenderId()).add(messageId);
        Type originalType = message.getType();
        // Message already received (add returns false if already exists) => Discard
        if (isRepeated) {
            message.setType(Message.Type.IGNORE);
        }

        switch (message.getType()) {
            case APPEND -> { // Client -> Server -> Client
                //return message;
            }
            case PRE_PREPARE -> {
                return message;
            }
            case IGNORE -> {
                if (!originalType.equals(Type.COMMIT))
                    return message;
            }
            case PREPARE -> {
                ConsensusMessage consensusMessage = (ConsensusMessage) message;
                if (consensusMessage.getReplyTo() != null && consensusMessage.getReplyTo().equals(config.getId()))
                    receivedAcks.add(consensusMessage.getReplyToMessageId());

                return message;
            }
            case COMMIT -> {
                ConsensusMessage consensusMessage = (ConsensusMessage) message;
                if (consensusMessage.getReplyTo() != null && consensusMessage.getReplyTo().equals(config.getId()))
                    receivedAcks.add(consensusMessage.getReplyToMessageId());
            }
            default -> {}
        }

        if (!local) {
            InetAddress address = InetAddress.getByName(response.getAddress().getHostAddress());
            int port = response.getPort();

            Message responseMessage = new Message(this.config.getId(), Message.Type.ACK);
            responseMessage.setMessageId(messageId);
            // sign the ACK
            byte[] buf = new Gson().toJson(responseMessage).getBytes();
            byte[] signature = sign(buf);
            // ACK is sent without needing for another ACK because
            // we're assuming an eventually synchronous network
            // Even if a node receives the message multiple times,
            // it will discard duplicates

            unreliableSend(address, port, responseMessage, signature);
        }
        
        return message;
    }
    // Close socket
    public void close() {
        // Close socket in a way where it doesnt send a expection if someone is receiving from this socket
        try {
            socket.close();
            socket.disconnect();
        } catch (Exception e) {
            // ignore
        }
    }
}
