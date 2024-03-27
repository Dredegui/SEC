package pt.ulisboa.tecnico.hdsledger.utilities;

import java.util.UUID;

public class Transaction {
    // public key hash of the sender
    private String sender;
    // public key hash of the receiver
    private String receiver;
    private double amount;
    private byte[] senderSignature;
    private int nonce;
    private String transactionId;


    public Transaction(String sender, String receiver, double amount, byte[] senderSignature, int nonce) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.senderSignature = senderSignature;
        this.nonce = nonce;
        this.transactionId = UUID.randomUUID().toString();  // unique id
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

    public int getNonce() {
        return nonce;
    }

    public byte[] getSenderSignature() {
        return senderSignature;
    }

    
}
