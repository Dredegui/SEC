package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class TransferMessage {

    // Source public key hash
    private String source;
    // Destiny public key hash
    private String destination;
    private double amount;
    private byte[] signature;
    private int nonce;

    public TransferMessage(String source, String destination, double amount, byte[] signature) {
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.signature = signature;
        this.nonce = 0;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public double getAmount() {
        return amount;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getNonce() {
        return nonce;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
    
}
