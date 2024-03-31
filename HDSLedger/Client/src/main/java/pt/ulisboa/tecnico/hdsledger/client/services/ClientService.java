package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

import pt.ulisboa.tecnico.hdsledger.client.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.TransferMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.communication.Link;



public class ClientService {

    private Link link;
    private int nonce;
    private String privateKey;

    private final MessageBucket checkBalanceMessages;
    private final MessageBucket transferConfirmationMessages;
    private final MessageBucket appendConfirmationMessages;

    private List<Integer> nonces = new ArrayList<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private int totalNodes;

    public ClientService(Link link, String privateKeyPath, ProcessConfig[] nodesConfig) {
        this.link = link;
        this.privateKey = privateKeyPath;
        this.nonce = 0;
        int nodeCount = 0;
        for (int i = 0; i < nodesConfig.length; i++) {
            if (!nodesConfig[i].getId().contains("client")) {
                nodeCount++;
            }
        }
        this.totalNodes = nodeCount;
        try {
            nonce = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.checkBalanceMessages = new MessageBucket(nodeCount);
        this.transferConfirmationMessages = new MessageBucket(nodeCount);
        this.appendConfirmationMessages = new MessageBucket(nodeCount);

    }

    public void append(String data, String id) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.APPEND);
        nonce++;
        byte[] dataNonce = (data + nonce).getBytes();
        byte[] signature = CryptSignature.sign(dataNonce, privateKey);
        consensusMessage.setSignature(signature);
        consensusMessage.setMessage(new AppendMessage(data, nonce).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");

    }

    public void check_balance(String id, String publicKeyHash) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.CHECK_BALANCE);

        consensusMessage.setMessage(new CheckBalanceMessage(publicKeyHash).toJson());

        this.link.broadcast(consensusMessage);

    }

    public void transfer(String id, String sourcePublicKeyHash, String destinyPublicKeyHash, double amount) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.TRANSFER);
        nonce++;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, privateKey);
        consensusMessage.setSignature(signature);
        consensusMessage.setMessage(new TransferMessage(sourcePublicKeyHash, destinyPublicKeyHash, amount, nonce).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");

        //this.listenTranfers();
        
    }

    private void checkBalanceListen(Message message) {

        ConsensusMessage consensusMessage = ((ConsensusMessage) message);
        CheckBalanceMessage checkBalanceMessage = consensusMessage.deserializeCheckBalanceMessage();

        checkBalanceMessages.addCheckBalanceMessage(checkBalanceMessage);

        Optional<String> quorumResult = checkBalanceMessages.hasValidCheckBalanceQuorum();
        if (quorumResult.isPresent()) {
            // O quórum está presente, extraia os saldos do Optional
            String[] parts = quorumResult.get().split(":");
            if (parts.length >= 2) {
                String authorizedBalance = parts[0];
                String contabilisticBalance = parts[1];

                System.out.println("Your authorized balance is: " + authorizedBalance + " your contabilistic balance is: " + contabilisticBalance);
                checkBalanceMessages.clearCheckBalanceMessages();
            }
        }
    }
    
    private void tranfersListen(Message message) {
        ConsensusMessage consensusMessage = ((ConsensusMessage) message);
        ConfirmationMessage confirmationMessage = consensusMessage.deserializeConfirmationMessage();

        // Adicione a mensagem à sua coleção
        transferConfirmationMessages.addTransferConfirmationMessage(confirmationMessage);

        // Verifique se um quórum foi alcançado sem interromper a escuta

        int messageNonce = confirmationMessage.getNonce();

        if(nonces.contains(messageNonce)) {
            return;
        }
        
        Optional<Integer> quorumResult = transferConfirmationMessages.hasValidTransferConfirmationQuorom(messageNonce);
        if (quorumResult.isPresent()) {
            // O quórum está presente, extraia os saldos do Optional
            int ledgerLocation = quorumResult.get();

            if (ledgerLocation == -1) {
                System.out.println("Transfer failed: Invalid destiny account");
            }
            else if (ledgerLocation == -2) {
                System.out.println("Transfer failed: Insufficient funds");
            }
            else {
                System.out.println("Your ledger location is: " + ledgerLocation + " for nonce: " + messageNonce);
                nonces.add(messageNonce);
            }
        }
    }

    public void infiniteListen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {
                        
                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case CONFIRMATION -> {
                                    tranfersListen(message);
                                }

                                case CHECK_BALANCE -> {
                                    checkBalanceListen(message);
                                }

                                case ACK -> {}

                                case IGNORE -> {}

                                default ->
                                    System.out.println("Received unkown message ");

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}