package pt.ulisboa.tecnico.hdsledger.communication;

import javax.swing.TransferHandler.TransferSupport;

import com.google.gson.Gson;

public class ConsensusMessage extends Message {

    // Consensus instance
    private int consensusInstance;
    // Round
    private int round;
    // Who sent the previous message
    private String replyTo;
    // Id of the previous message
    private int replyToMessageId;
    // Message (PREPREPARE, PREPARE, COMMIT, CHECK_BALANCE, APPEND, TRANSFER, CONFIRMATION)
    private String message;
    // nonce from client
    private int nonce;
    // data signed by client
    private byte[] signature;
    // client id
    private String clientId;

    public ConsensusMessage(String senderId, Type type) {
        super(senderId, type);
    }

    public TransferMessage deserializeTransferMessage() {
        return new Gson().fromJson(this.message, TransferMessage.class);
    }

    public CheckBalanceMessage deserializeCheckBalanceMessage() {
        return new Gson().fromJson(this.message, CheckBalanceMessage.class);
    }

    public ConfirmationMessage deserializeConfirmationMessage() {
        return new Gson().fromJson(this.message, ConfirmationMessage.class);
    }
    
    public PrePrepareMessage deserializePrePrepareMessage() {
        return new Gson().fromJson(this.message, PrePrepareMessage.class);
    }

    public PrepareMessage deserializePrepareMessage() {
        return new Gson().fromJson(this.message, PrepareMessage.class);
    }

    public CommitMessage deserializeCommitMessage() {
        return new Gson().fromJson(this.message, CommitMessage.class);
    }

    public AppendMessage deserializeAppendMessage() {
        return new Gson().fromJson(this.message, AppendMessage.class);
    }

    public RoundChangeMessage deserializeRoundChangeMessage() {
        return new Gson().fromJson(this.message, RoundChangeMessage.class);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getConsensusInstance() {
        return consensusInstance;
    }

    public void setConsensusInstance(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public int getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(int replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }
}
