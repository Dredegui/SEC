package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.util.Optional;

import com.google.gson.Gson;

import pt.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.ErrorMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.communication.Link;



public class ClientService {

    private Link link;
    private int nonce;
    private String privateKey;
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    private final MessageBucket checkBalanceMessages;

    public ClientService(Link link, String privateKeyPath, ProcessConfig[] nodesConfig) {
        this.link = link;
        this.privateKey = privateKeyPath;
        this.nodesConfig = nodesConfig;
        int nodeCount = 0;
        for (int i = 0; i < nodesConfig.length; i++) {
            if (!nodesConfig[i].getId().contains("client")) {
                nodeCount++;
            }
        }
        
        try {
            nonce = 0;
        } catch (Exception e) {
            e.printStackTrace();
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

                    // Add the message to your collection
                    checkBalanceMessages.addCheckBalanceMessage(checkBalanceMessage);

                    // Now, check for a quorum
                    Optional<Double[]> quorumResult = checkBalanceMessages.hasValidCheckBalanceQuorum();
                    if (quorumResult.isPresent()) {
                        // Quorum is present, extract the balances from the Optional
                        Double[] balances = quorumResult.get();
                        Double authorizedBalance = balances[0];
                        Double contabilisticBalance = balances[1];

                        // Output the quorum balances
                        System.out.println("Quorum reached.");
                        System.out.println("Your authorized balance is: " + authorizedBalance);
                        System.out.println("Your contabilistic balance is: " + contabilisticBalance);

                        listen = false; // Stop listening as quorum is achieved
                    } else {
                        // Output a message indicating you're waiting for a quorum
                        System.out.println("Waiting for a quorum...");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}