package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class TransferMessage {

    // Source public key hash
    private String source;
    // Destiny public key hash
    private String destiny;
    private double amount;
    private int nonce;

    public TransferMessage(String source, String destiny, double amount, int nonce) {
        this.source = source;
        this.destiny = destiny;
        this.amount = amount;
        this.nonce = nonce;
    }

    public String getSource() {
        return source;
    }

    public String getDestiny() {
        return destiny;
    }

    public double getAmount() {
        return amount;
    }

    public int getNonce() {
        return nonce;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
    
}
