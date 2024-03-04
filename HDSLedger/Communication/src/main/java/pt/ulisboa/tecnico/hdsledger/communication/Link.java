package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.Message.Type;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
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
            this.nodes.put(id, node);
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
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
	}

    public static PrivateKey getPrivateKey(String privateKeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new HDSSException(ErrorMessage.ExtractKeyError);
        }
    }

    // Function that generates a key signature from a value
    public String sign(String nodeId, String value) {
        // Other node public key
        String digitalSignature = "";
        // others node public .key file path
        String publicKey = nodes.get(nodeId).getPublicKey();
        //extract public key from .key file
        PublicKey pubKey = getPublicKey(publicKey);
        // current node private key
        PrivateKey privKey = getPrivateKey(privateKey);
        try {
            // Create a Signature object and initialize it with the private key
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(privKey);
            // Update and sign the data
            rsa.update(value.getBytes());
            byte[] signature = rsa.sign();
            digitalSignature = Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
        }
        // Encrypt digital singature with other node public key
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encryptedSignature = cipher.doFinal(digitalSignature.getBytes());
            digitalSignature = Base64.getEncoder().encodeToString(encryptedSignature);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
        }
        return digitalSignature;
    }

    // inverse of the sign function aka validate
    public boolean validate(String nodeId, String value, String signature) {
        // Other node public key
        String publicKey = nodes.get(nodeId).getPublicKey();
        //extract public key from .key file
        PublicKey pubKey = getPublicKey(publicKey);
        // current node private key
        PrivateKey privKey = getPrivateKey(privateKey);
        try {
            // Decrypt digital singature with current node private key
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] decryptedSignature = cipher.doFinal(Base64.getDecoder().decode(signature));
            signature = new String(decryptedSignature);
        } catch (Exception e) { // TODO: improve exception handling and specification
            e.printStackTrace();
        }
        // validate signature
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(pubKey);
            rsa.update(value.getBytes());
            return rsa.verify(Base64.getDecoder().decode(signature));
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

                for (;;) {
                    LOGGER.log(Level.INFO, MessageFormat.format(
                            "{0} - Sending {1} message to {2}:{3} with message ID {4} - Attempt #{5}", config.getId(),
                            data.getType(), destAddress, destPort, messageId, count++));

                    unreliableSend(destAddress, destPort, data);

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
    public void unreliableSend(InetAddress hostname, int port, Message data) {
        new Thread(() -> {
            try {
                byte[] buf = new Gson().toJson(data).getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, hostname, port);
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
            byte[] buf = new byte[65535];
            response = new DatagramPacket(buf, buf.length);

            socket.receive(response);

            byte[] buffer = Arrays.copyOfRange(response.getData(), 0, response.getLength());
            serialized = new String(buffer);
            message = new Gson().fromJson(serialized, Message.class);
        }

        String senderId = message.getSenderId();
        int messageId = message.getMessageId();

        if (!nodes.containsKey(senderId))
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

            // ACK is sent without needing for another ACK because
            // we're assuming an eventually synchronous network
            // Even if a node receives the message multiple times,
            // it will discard duplicates
            unreliableSend(address, port, responseMessage);
        }
        
        return message;
    }
}
