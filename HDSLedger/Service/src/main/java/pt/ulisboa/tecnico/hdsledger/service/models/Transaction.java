package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.UUID;

public class Transaction {
    private String sender;
    private String receiver;
    private double amount;
    private String transactionId;
    private long timestamp;

    public Transaction(String sender, String receiver, double amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.transactionId = UUID.randomUUID().toString();  // unique id
        this.timestamp = System.currentTimeMillis(); 
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
