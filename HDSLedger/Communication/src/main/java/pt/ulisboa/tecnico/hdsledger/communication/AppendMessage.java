package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class AppendMessage {

    // Value
    private String value;
    // Nonce
    private int nonce;

    // This is a client id
    public AppendMessage(String value, int nonce) {
        this.value = value;
        this.nonce = nonce;
    }

    public String getValue() {
        return value;
    }

    public int getNonce() {
        return nonce;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
