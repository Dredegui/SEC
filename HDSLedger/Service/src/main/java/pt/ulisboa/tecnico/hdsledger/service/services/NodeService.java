package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Timer;
import java.util.TimerTask;

import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.PrePrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.communication.TransferMessage;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.models.BlockChain;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeService implements UDPService {

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    // Current node is leader
    private final ProcessConfig config;
    // Leader configuration
    private final ProcessConfig leaderConfig;

    // Link to communicate with nodes
    private final Link link;

    // Consensus instance -> Round -> List of prepare messages
    private final MessageBucket prepareMessages;
    // Consensus instance -> Round -> List of commit messages
    private final MessageBucket commitMessages;
    // Consensus instance -> Round -> List of round change messages
    private final MessageBucket roundChangeMessages;

    // Store if already received pre-prepare for a given <consensus, round>
    private final Map<Integer, Map<Integer, Boolean>> receivedPrePrepare = new ConcurrentHashMap<>();
    // Consensus instance information per consensus instance
    private final Map<Integer, InstanceInfo> instanceInfo = new ConcurrentHashMap<>();
    // Current consensus instance
    private final AtomicInteger consensusInstance = new AtomicInteger(0);
    // Last decided consensus instance
    private final AtomicInteger lastDecidedConsensusInstance = new AtomicInteger(0);
    // Delay for the timer
    private final int delay = 1000;

    // Ledger (for now, just a list of strings)
    private ArrayList<String> ledger = new ArrayList<String>();

    // BlockChain
    private BlockChain blockChain = new BlockChain();

    // Account map (public key -> account)
    private Map<String, Account> accounts = new ConcurrentHashMap<>();

    private Timer timer;
    
    private TimerTask task;

    public NodeService(Link link, ProcessConfig config,
            ProcessConfig leaderConfig, ProcessConfig[] nodesConfig) {

        this.link = link;
        this.config = config;
        this.leaderConfig = leaderConfig;
        this.nodesConfig = nodesConfig;
        // count number of nodes that aren't client by checking if string client is in node.getId
        int nodeCount = 0;
        for (int i = 0; i < nodesConfig.length; i++) {
            if (!nodesConfig[i].getId().contains("client")) {
                nodeCount++;
            }
            else {
                try{

                    String publicKey = CryptSignature.loadPublicKey(nodesConfig[i].getPublicKey());
                    String publicKeyHash = CryptSignature.hashPublicKey(publicKey);
                    accounts.put(publicKeyHash, new Account(publicKeyHash));

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        }
        this.prepareMessages = new MessageBucket(nodeCount);
        this.commitMessages = new MessageBucket(nodeCount);
        this.roundChangeMessages = new MessageBucket(nodeCount);

        this.timer = new Timer();
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public int getConsensusInstanceRound(int instance) {
        return this.instanceInfo.get(instance).getCurrentRound();
    }

    public ArrayList<String> getLedger() {
        return this.ledger;
    }

    public BlockChain getBlockChain() {
        return this.blockChain;
    }

    private void updateAllLeader(int round) {
        for (int i = 1; i <= nodesConfig.length; i++) {
            nodesConfig[i - 1].updateLeader(round);
        }
        config.updateLeader(round);
    }

    public boolean isLeader(String id) {
        System.out.println("ID: " + id + "Actual id: " + nodesConfig[Integer.parseInt(id) - 1].getId() + "Leader: " + nodesConfig[Integer.parseInt(id) - 1].isLeader());
        return nodesConfig[Integer.parseInt(id) - 1].isLeader();
    }
 
    public ConsensusMessage createConsensusMessage(String value, int instance, int round, int nonce, byte[] signature, String clientId) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .setClientInfo(nonce, signature, clientId)
                .build();

        return consensusMessage;
    }

    // Validate AppendMessage signature
    public boolean validateMessageClientSignature(ConsensusMessage consensusMessage, String value) {
        if (value == null) {
            return true;
        }
        int nonce = consensusMessage.getNonce();
        byte[] signature = consensusMessage.getSignature();
        String clientId = consensusMessage.getClientId();
        byte[] data = (value + nonce).getBytes();
        String publicKey = null;
        // Print all information
        for (int i = 0; i < nodesConfig.length; i++) {
            if (nodesConfig[i].getId().equals(clientId)) {
                publicKey = nodesConfig[i].getPublicKey();
                break;
            }
        }
        return CryptSignature.validate(data, signature, publicKey);
    }

    public ConsensusMessage createRoundChange(int instance, int round, int preparedRound, String preparedValue) {
        RoundChangeMessage roundChangeMessage = new RoundChangeMessage(preparedRound, preparedValue);
        InstanceInfo info = this.instanceInfo.get(instance);
        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setClientInfo(info.getNonce(), info.getSignature(), info.getSenderId())
                .setMessage(roundChangeMessage.toJson())
                .build();

        return consensusMessage;
    }

    public synchronized void activateTimer(long delay, int round) {
        if (task != null) {
            task.cancel();
        }

        task = new TimerTask() {
            @Override
            public void run() {
                uponTimer();
            }
        };

        timer.schedule(task, (long)Math.pow(delay, round));
    }

    private void uponTimer() {
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Timer expired", config.getId()));

        // Increment the round ri = ri + 1
        InstanceInfo info = this.instanceInfo.get(this.consensusInstance.get());
        info.setCurrentRound(info.getCurrentRound() + 1);

        //  Set timer to running and expire after delay with a exponential of round from info
        activateTimer(delay, info.getCurrentRound());
        
        // Broadcast (ROUND-CHANGE, λ, r, pr, pv)
        String msg = MessageFormat.format("{0} - Broadcasting ROUND-CHANGE message for Consensus Instance {1}, Round {2}, Prepared Round {3}, Prepared Value {4}",
                config.getId(), this.consensusInstance.get(), info.getCurrentRound(), info.getPreparedRound(), info.getPreparedValue());
        System.out.println(msg);
        ConsensusMessage roundChangeMessage = this.createRoundChange(this.consensusInstance.get(), info.getCurrentRound(), info.getPreparedRound(), info.getPreparedValue());
        this.link.broadcast(roundChangeMessage);
    }

    public synchronized void stopTimer() {
        if (task != null) {
            task.cancel();
            timer.purge();
        }
    }

    public boolean justifyRoundChange(int instance, int round, int preparedRound) { 
        boolean everyNull =  roundChangeMessages.allNullRoundChange(instance, round);
        if (!everyNull) {
            Optional<String> validQuorum = prepareMessages.hasValidPrepareQuorum(null, instance, preparedRound);
            Optional<Pair<Integer, String>> highestPrepared = roundChangeMessages.getHighestPreparedRound(instance, round);
            boolean checkHighestPrepared = false;
            if (validQuorum.isPresent() && highestPrepared.isPresent()) {
                // check if every (pr, pv) pair from each prepare messages is equal to (pr, pv) from the highest prepared round
                checkHighestPrepared = prepareMessages.checkHighestPrepared(instance, preparedRound, highestPrepared.get().getLeft(), highestPrepared.get().getRight()); 
            }
            return checkHighestPrepared;
        }
        return everyNull;
    }

    public boolean justifyPrePrepare(int instance, int round) {
        System.out.println("Justify pre prepare");
        if (round == 1) {
            return true;
        }
        Optional<Integer> validQuorum = roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), instance, round);
        return validQuorum.isPresent() && justifyRoundChange(instance, round, validQuorum.get());
    }

    public ConsensusMessage findMessage(Collection<ConsensusMessage> messages, String value) {
        for (ConsensusMessage m : messages) {
            // get value if it's a prepare message or a commit message
            if (m.getType() == Message.Type.PREPARE) {
                PrepareMessage pM = m.deserializePrepareMessage();
                if (pM.getValue().equals(value)) {
                    return m;
                }
            } else if (m.getType() == Message.Type.COMMIT) {
                CommitMessage cM = m.deserializeCommitMessage();
                if (cM.getValue().equals(value)) {
                    return m;
                }
            }
        }
        throw new RuntimeException("Message not found");
    }

    public void uponRoundChange(ConsensusMessage message) {
        // Save round change message in a bucket until f+1 messages are received
        InstanceInfo info = this.instanceInfo.get(this.consensusInstance.get());
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        int currentRound = info.getCurrentRound();
        String senderId = message.getSenderId();
        String msg1 = MessageFormat.format(
            "{0} - Received ROUND-CHANGE message from {1} Consensus Instance {2}, Round {3}",
            config.getId(), senderId, consensusInstance, round);
        System.out.println(msg1);
        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received ROUND-CHANGE message from {1} Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));
        RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
        if (!validateMessageClientSignature(message, roundChangeMessage.getPreparedValue())) {
            LOGGER.log(Level.WARNING, MessageFormat.format("{0} - RoundChange Message value doesn't match signature from client {1}", config.getId(), message.getClientId()));
            return;
        }
        int preparedRound = roundChangeMessage.getPreparedRound();
        roundChangeMessages.addMessage(message);
        // If f+1 messages are received, update current round with the lowest round from the round change messages
        int minRound = roundChangeMessages.getRoundChangeMinRound(consensusInstance, currentRound, round);
        if (minRound != -1 && currentRound < round) {
            // valors = funcao obter min
            info.setCurrentRound(minRound);
            activateTimer(delay, info.getCurrentRound());
            link.broadcast(this.createRoundChange(consensusInstance, minRound, info.getPreparedRound(), info.getPreparedValue()));
        } else if (justifyRoundChange(consensusInstance, round, preparedRound) && roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round).isPresent() && this.config.isLeader(round)) {
            updateAllLeader(round);
            String msg = MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId());
            System.out.println(msg);
            Optional<Pair<Integer, String>> highestPrepared = roundChangeMessages.getHighestPreparedRound(consensusInstance, round);
            String value = null;
            Message appendMessage = null;
            if (highestPrepared.isPresent()) {
                value = highestPrepared.get().getRight();
                info.setPreparedRound(highestPrepared.get().getLeft());
                info.setPreparedValue(value);
                Collection<ConsensusMessage> messages = prepareMessages.getMessages(consensusInstance, highestPrepared.get().getLeft()).values();
                // find the append message associated with the highest prepared round
                ConsensusMessage m = findMessage(messages, value);
                info.setNonce(m.getNonce());
                info.setSignature(m.getSignature());
                info.setSenderId(m.getClientId());
            } else {
                value = info.getInputValue();
            }
            // get the append message associated with the highest prepared round and value
            this.link.broadcast(this.createConsensusMessage(value, consensusInstance, round, info.getNonce(), info.getSignature(), info.getSenderId()));
        }
        // If my node already decided on the consensus instance, send commit message
        if (lastDecidedConsensusInstance.get() >= consensusInstance) {
            InstanceInfo instance = this.instanceInfo.get(consensusInstance);
            LOGGER.log(Level.INFO,
                        MessageFormat.format(
                                "{0} - Already decided on Consensus Instance {1}, Round {2}, sending COMMIT message",
                                config.getId(), consensusInstance, round));
            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
            .setConsensusInstance(consensusInstance)
            .setRound(round)
            .setReplyTo(message.getSenderId())
            .setReplyToMessageId(message.getMessageId())
            .setMessage(instance.getCommitMessage().toJson())
            .build();
            link.send(message.getSenderId(), m);
        }
    }


    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     * @param clientId client that started the consensus
     */
    public void startConsensus(String value, int nonce, byte[]signature, String clientId) {

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(value));
        String msg = MessageFormat.format("{0} - Starting consensus for instance {1} with value {2}",
                config.getId(), localConsensusInstance, value);
        System.out.println(msg);
        // If startConsensus was already called for a given round
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    config.getId(), localConsensusInstance));
            return;
        }
        
        // Only start a consensus instance if the last one was decided
        // We need to be sure that the previous value has been decided
        while (lastDecidedConsensusInstance.get() < localConsensusInstance - 1) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        updateAllLeader(getConsensusInstanceRound(localConsensusInstance));
        InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);
        instance.setNonce(nonce);
        instance.setSignature(signature);
        instance.setSenderId(clientId);
        // Leader broadcasts PRE-PREPARE message
        if (this.config.isLeader()) {
            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));
            this.link.broadcast(this.createConsensusMessage(value, localConsensusInstance, instance.getCurrentRound(), nonce, signature, clientId));
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }
        activateTimer(delay, instance.getCurrentRound());
    }

    public void uponCheckBalance(ConsensusMessage message){
        CheckBalanceMessage checkBalanceMessage = message.deserializeCheckBalanceMessage();

        Account account = accounts.get(checkBalanceMessage.getPublicKeyHash());

        String senderId = message.getSenderId();

        if (account!=null) { 

            double autorizedBalance = account.getAutorizedBalance();
            double contablisticBalance = account.getContablisticBalance();

            CheckBalanceMessage reply = new CheckBalanceMessage(autorizedBalance, contablisticBalance);

            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.CHECK_BALANCE)
                    .setMessage(reply.toJson())
                    .setReplyTo(senderId)
                    .build();

            link.send(senderId, consensusMessage);
        }

    }

    public void uponTransfer(ConsensusMessage message) {
        
        TransferMessage transferMessage = message.deserializeTransferMessage();

        String source = transferMessage.getSource();
        String destiny = transferMessage.getDestiny();
        double amount = transferMessage.getAmount();
        int nonce = transferMessage.getNonce();

        byte [] dataReceived = (source + destiny + amount + nonce).getBytes();
        byte [] signature = transferMessage.getSignature();
        // verify signature of data received
        // to do

        Account senderAccount = accounts.get(source);
        if (senderAccount != null) {
            System.out.println("Sender account: " + senderAccount.getPublicKeyHash());
        }

        Account receiverAccount = accounts.get(destiny);
        if (receiverAccount==null) {
            // send to the client a message saying that the transfer was not possible
            // because wrong destiny account
            // to do
        }
        System.out.println("Receiver account: " + receiverAccount.getPublicKeyHash());

        if (!senderAccount.hasEnoughAutorizedBalance(amount)) {
            // send to the client a message saying that the transfer was not possible
            // to do
        }

        else {
            // add the transaction to the list of current transactions
            blockChain.addTransaction(senderAccount.getPublicKeyHash(), receiverAccount.getPublicKeyHash(), amount, signature, nonce);

            if(blockChain.isReadyToProcessTransactions()) {

                // modify the startConsensus method to accept a list of transactions
                // startConsensus(blockChain.getCurrentTransactions(), message.getNonce(), message.getSignature(), message.getClientId());
                
            }
        }
        
    }
    

    /*
     * Handle append messages. Starts the consesus
     * with the value of the message
     * 
     * @param message Message to be handled
     */
    public void uponAppend(ConsensusMessage message) {

        AppendMessage appendMessage = message.deserializeAppendMessage();

        String value = appendMessage.getValue();
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received APPEND message: {1}", config.getId(), value));
        System.out.println("Received APPEND message: " + value + " from " + message.getSenderId() + " with nonce " + appendMessage.getNonce() + " and signature " + appendMessage.getSignature());
        startConsensus(value, appendMessage.getNonce(), appendMessage.getSignature(), message.getSenderId());
    }

    
    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId(); 
        int senderMessageId = message.getMessageId();
        PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();
        String value = prePrepareMessage.getValue();
        if (!validateMessageClientSignature(message, value)) {
            LOGGER.log(Level.WARNING, MessageFormat.format("{0} - PrePrepare Message value doesn't match signature from client {1}", config.getId(), message.getClientId()));
            return;
        }
        int currentRound = this.instanceInfo.get(consensusInstance).getCurrentRound();
        String msg = MessageFormat.format(
            "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}, Current Round {4}",
            config.getId(), senderId, consensusInstance, round, currentRound);
        System.out.println(msg);
        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));
        
        updateAllLeader(currentRound);
        // Verify if pre-prepare was sent by leader
        if (!isLeader(senderId)) {
            return;
        }
        if (justifyPrePrepare(consensusInstance, round)) {
            // Set instance value
            this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value));

            // Within an instance of the algorithm, each upon rule is triggered at most once
            // for any round r
            receivedPrePrepare.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
            if (receivedPrePrepare.get(consensusInstance).put(round, true) != null) {
                LOGGER.log(Level.INFO,
                        MessageFormat.format(
                                "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                                        + "replying again to make sure it reaches the initial sender",
                                config.getId(), consensusInstance, round));
            }

            PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());
            
            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
            .setConsensusInstance(consensusInstance)
            .setRound(round)
            .setClientInfo(message.getNonce(), message.getSignature(), message.getClientId())
            .setMessage(prepareMessage.toJson())
            .setReplyTo(senderId)
            .setReplyToMessageId(senderMessageId)
            .build();
            activateTimer(delay, round);

            this.link.broadcast(consensusMessage);
        }
    }

    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public synchronized void uponPrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        PrepareMessage prepareMessage = message.deserializePrepareMessage();

        String value = prepareMessage.getValue();
        if (!validateMessageClientSignature(message, value)) {
            LOGGER.log(Level.WARNING, MessageFormat.format("{0} - Prepare Message value doesn't match signature from client {1}", config.getId(), message.getClientId()));
            return;
        }
        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));
        String msg = MessageFormat.format(
            "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
            config.getId(), senderId, consensusInstance, round);
        System.out.println(msg);
        // Doesn't add duplicate messages
        prepareMessages.addMessage(message);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));
            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setClientInfo(instance.getNonce(), instance.getSignature(), instance.getSenderId())
                    .setMessage(instance.getCommitMessage().toJson())
                    .build();

            link.send(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<String> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {
            instance.setPreparedValue(preparedValue.get());
            instance.setPreparedRound(round);
            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = prepareMessages.getMessages(consensusInstance, round)
                    .values();
            ConsensusMessage temp = findMessage(sendersMessage, preparedValue.get());
            CommitMessage c = new CommitMessage(preparedValue.get());
            instance.setCommitMessage(c);
            sendersMessage.forEach(senderMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(senderMessage.getSenderId())
                        .setReplyToMessageId(senderMessage.getMessageId())
                        .setClientInfo(temp.getNonce(), temp.getSignature(), temp.getClientId())
                        .setMessage(c.toJson())
                        .build();

                link.send(senderMessage.getSenderId(), m);
            });
        }
    }



    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public synchronized void uponCommit(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String msg = MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                config.getId(), message.getSenderId(), consensusInstance, round);
        System.out.println(msg);
        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));
        if (!validateMessageClientSignature(message, message.deserializeCommitMessage().getValue())) {
            LOGGER.log(Level.WARNING, MessageFormat.format("{0} - Commit Message value doesn't match signature from client {1}", config.getId(), message.getClientId()));
            return;
        }
        
        commitMessages.addMessage(message);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), consensusInstance, round);
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }
        
        Optional<String> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);
        if (commitValue.isPresent() && instance.getCommittedRound() < round) {
            stopTimer();
            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String value = commitValue.get();
            ConsensusMessage m = findMessage(commitMessages.getMessages(consensusInstance, round).values(), value);
            // Append value to the ledger (must be synchronized to be thread-safe)
            synchronized(ledger) {
                if (ledger.size() < consensusInstance - 1) {
                    return;
                }
                // Increment size of ledger to accommodate current instance
                ledger.ensureCapacity(consensusInstance);
                while (ledger.size() < consensusInstance - 1) {
                    ledger.add("");
                }
            
                ledger.add(consensusInstance - 1, value);
                System.out.println("nodeID " + config.getId() + "COMMITED VALUE: " + value + " clientId: " + m.getClientId());

                // Apenas o lider é que envia a confirmation 
                if(this.config.isLeader()){
        
                    // Send to client confirmation message of the appended value to the ledger
                    this.link.send(m.getClientId(), new ConsensusMessageBuilder(this.config.getId(),Message.Type.CONFIRMATION)
                            .setMessage(new ConfirmationMessage(consensusInstance-1).toJson())
                            .build());
                }
                
                LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Current Ledger: {1}",
                            config.getId(), String.join("", ledger)));
            }

            lastDecidedConsensusInstance.getAndIncrement();
            System.out.println("Last decided consensus instance: " + lastDecidedConsensusInstance.get() + "NodeID: " + config.getId());
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            config.getId(), consensusInstance, round, true));
        }
    }

    // Close link socket
    public void close() {
        link.close();
    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {
                        
                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case TRANSFER ->
                                    uponTransfer((ConsensusMessage) message);

                                case CHECK_BALANCE ->
                                    uponCheckBalance((ConsensusMessage) message);

                                case ROUND_CHANGE ->
                                    uponRoundChange((ConsensusMessage) message);

                                case APPEND ->
                                    uponAppend((ConsensusMessage) message);

                                case PRE_PREPARE ->
                                    uponPrePrepare((ConsensusMessage) message);

                                case PREPARE ->
                                    uponPrepare((ConsensusMessage) message);


                                case COMMIT ->
                                    uponCommit((ConsensusMessage) message);


                                case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            config.getId(), message.getSenderId()));

                                case IGNORE ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    // if excpetion is a socket close, ignore (this was done for testing purposes)
                    if (!e.getMessage().equals("Socket closed")) {
                        e.printStackTrace();
                    } else {
                        // close current thread
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}