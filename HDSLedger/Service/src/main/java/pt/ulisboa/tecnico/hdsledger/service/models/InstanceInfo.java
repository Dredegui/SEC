package pt.ulisboa.tecnico.hdsledger.service.models;


import java.util.List;

import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private String preparedValue;
    private CommitMessage commitMessage;
    private String inputValue;
    private int committedRound = -1;
    private String senderId = null;
    private int nonce;
    private byte[] signature;
    private List<Transaction> currentTransactions;

    // Contructor for transactions
    public InstanceInfo(List<Transaction> currentTransactions) {
        this.currentTransactions = currentTransactions;
    }

    public InstanceInfo(String inputValue) {
        this.inputValue = inputValue;
    }

    public List<Transaction> getCurrentTransactions() {
         return this.currentTransactions;
    }

    public void setCurrentTransactions(List<Transaction> currentTransactions) {
        this.currentTransactions = currentTransactions;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public void setPreparedValue(String preparedValue) {
        this.preparedValue = preparedValue;
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public int getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(int committedRound) {
        this.committedRound = committedRound;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(CommitMessage commitMessage) {
        this.commitMessage = commitMessage;
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

}
