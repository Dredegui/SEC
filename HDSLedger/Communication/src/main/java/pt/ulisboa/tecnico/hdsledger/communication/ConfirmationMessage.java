package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class ConfirmationMessage {
    
    // Value
    private int ledgerMessageLocation;

    public ConfirmationMessage(int ledgerMessageLocation) {
        this.ledgerMessageLocation = ledgerMessageLocation;
    }

    public int getLedgerMessageLocation() {
        return this.ledgerMessageLocation;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

}
