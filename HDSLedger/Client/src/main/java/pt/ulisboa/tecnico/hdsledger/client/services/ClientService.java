package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        listenTranfers();
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
        
        this.listenAppends();

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
        consensusMessage.setSignature(signature);
        consensusMessage.setMessage(new TransferMessage(sourcePublicKeyHash, destinyPublicKeyHash, amount, nonce).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");

        //this.listenTranfers();
        
    }

    public void listenAppends() {
        try {
            Set<ConfirmationMessage> respondedNodes = new HashSet<>();
            boolean quorumReached = false;

            while (respondedNodes.size() < totalNodes) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CONFIRMATION) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    ConfirmationMessage confirmationMessage = consensusMessage.deserializeConfirmationMessage();
                    respondedNodes.add(confirmationMessage);

                    appendConfirmationMessages.addAppendConfirmationMessage(confirmationMessage);

                    if (!quorumReached) {
                        Optional<Integer> quorumResult = appendConfirmationMessages.hasValidAppendConfirmationQuorom();
                        if (quorumResult.isPresent()) {
                            quorumReached = true;

                            Integer ledgerLocation = quorumResult.get();

                            System.out.println("Quorum reached.");
                            System.out.println("Your ledger location is: " + ledgerLocation);
                        }
                    }

                    if (!quorumReached) {
                        System.out.println("Waiting for a quorum...");
                    }

                }
            }

            System.out.println("All nodes have responded.");
            appendConfirmationMessages.clearAppendConfirmationMessage();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void listenBalance() {
        try {
            Set<String> respondedNodes = new HashSet<>();
            boolean quorumReached = false;
            
            while (respondedNodes.size() < totalNodes) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CHECK_BALANCE) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    CheckBalanceMessage checkBalanceMessage = consensusMessage.deserializeCheckBalanceMessage();

                    respondedNodes.add(consensusMessage.getSenderId());

                    checkBalanceMessages.addCheckBalanceMessage(checkBalanceMessage);

                    if (!quorumReached) {
                        Optional<Double[]> quorumResult = checkBalanceMessages.hasValidCheckBalanceQuorum();
                        if (quorumResult.isPresent()) {
                            quorumReached = true;
                            // O quórum está presente, extraia os saldos do Optional
                            Double[] balances = quorumResult.get();
                            Double authorizedBalance = balances[0];
                            Double contabilisticBalance = balances[1];

                            // Saída dos saldos do quórum
                            System.out.println("Quorum reached.");
                            System.out.println("Your authorized balance is: " + authorizedBalance);
                            System.out.println("Your contabilistic balance is: " + contabilisticBalance);
                        }
                    }

                    // Continue a saída para esperar pelo quórum se ainda não tiver sido alcançado
                    if (!quorumReached) {
                        System.out.println("Waiting for a quorum...");
                    }
                }
            }

            // Todos os nós responderam
            System.out.println("All nodes have responded.");
            checkBalanceMessages.clearCheckBalanceMessages();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void listenTranfers() {
        executorService.submit(() -> {
            try {
                Set<ConfirmationMessage> respondedNodes = new HashSet<>();
                boolean quorumReached = false;
                
                while (respondedNodes.size() < totalNodes) {
                    Message message = link.receive();
                    if (message.getType() == Message.Type.CONFIRMATION) {
                        ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                        ConfirmationMessage confirmationMessage = consensusMessage.deserializeConfirmationMessage();
                        respondedNodes.add(confirmationMessage); 

                        // Adicione a mensagem à sua coleção
                        transferConfirmationMessages.addTransferConfirmationMessage(confirmationMessage);

                        // Verifique se um quórum foi alcançado sem interromper a escuta
                        if (!quorumReached) {
                            Optional<Integer> quorumResult = transferConfirmationMessages.hasValidTransferConfirmationQuorom();
                            if (quorumResult.isPresent()) {
                                quorumReached = true;
                                // O quórum está presente, extraia os saldos do Optional
                                Integer ledgerLocation = quorumResult.get();

                                if (ledgerLocation == -1) {
                                    System.out.println("Transfer failed: Invalid destiny account");
                                }
                                else if (ledgerLocation == -2) {
                                    System.out.println("Transfer failed: Insufficient funds");
                                }
                                else {
                                    // Saída dos saldos do quórum
                                    System.out.println("Quorum reached.");
                                    System.out.println("Your ledger location is: " + ledgerLocation);
                                }
                            }
                        }

                        // Continue a saída para esperar pelo quórum se ainda não tiver sido alcançado
                        if (!quorumReached) {
                            System.out.println("Waiting for a quorum...");
                        }

                    }
                }

                // Todos os nós responderam
                System.out.println("All nodes have responded.");
                transferConfirmationMessages.clearTransferConfirmationMessage();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

}