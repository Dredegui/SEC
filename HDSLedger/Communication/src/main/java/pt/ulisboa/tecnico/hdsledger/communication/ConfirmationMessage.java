package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class ConfirmationMessage {
    
    // Value
    private int ledgerMessageLocation;

    private int nonce;

    public ConfirmationMessage(int ledgerMessageLocation, int nonce) {
        this.ledgerMessageLocation = ledgerMessageLocation;
        this.nonce = nonce;
    }

    public int getLedgerMessageLocation() {
        return this.ledgerMessageLocation;
    }

    public int getNonce() {
        return this.nonce;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}
