package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.util.Optional;

import com.google.gson.Gson;

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
        this.checkBalanceMessages = new MessageBucket(nodeCount);
    }

    public void append(String data, String id) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.APPEND);
        nonce++;
        byte[] dataNonce = (data + nonce).getBytes();
        byte[] signature = CryptSignature.sign(dataNonce, privateKey);
        consensusMessage.setMessage(new AppendMessage(data, nonce, signature).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");
        
        this.listenConfirmation();

    }

    public void check_balance(String id, String publicKeyHash) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.CHECK_BALANCE);

        consensusMessage.setMessage(new CheckBalanceMessage(publicKeyHash).toJson());

        this.link.broadcast(consensusMessage);
        
        this.listenBalance();

    }

    public void transfer(String id, String sourcePublicKeyHash, String destinyPublicKeyHash, double amount) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.TRANSFER);
        nonce++;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, privateKey);

        consensusMessage.setMessage(new TransferMessage(sourcePublicKeyHash, destinyPublicKeyHash, amount, signature, nonce).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");
        
    }

    public void listenConfirmation() {
        
        try {
            boolean listen = true;
            while (listen) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CONFIRMATION) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    ConfirmationMessage confirmationMessage = consensusMessage.deserializeConfirmationMessage();
                    System.out.println("Message appended to the chain successfully in position: " + confirmationMessage.getLedgerMessageLocation());
                    listen = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
    }

    public void listenBalance() {
        try {
            boolean listen = true;
            while (listen) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CHECK_BALANCE) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    CheckBalanceMessage checkBalanceMessage = consensusMessage.deserializeCheckBalanceMessage();

                    checkBalanceMessages.addCheckBalanceMessage(checkBalanceMessage);

                    // Check if a quorum is reached
                    Optional<Double[]> quorumResult = checkBalanceMessages.hasValidCheckBalanceQuorum();
                    if (quorumResult.isPresent()) {
                        Double[] balances = quorumResult.get();
                        Double authorizedBalance = balances[0];
                        Double contabilisticBalance = balances[1];

                        System.out.println("Quorum reached!");
                        System.out.println("Your authorized balance is: " + authorizedBalance);
                        System.out.println("Your contabilistic balance is: " + contabilisticBalance);

                        checkBalanceMessages.clearCheckBalanceMessages();

                        listen = false; 
                    } else {
                        
                        System.out.println("Waiting for a quorum...");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}