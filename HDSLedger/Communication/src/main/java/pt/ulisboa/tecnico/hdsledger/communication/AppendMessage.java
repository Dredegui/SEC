package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class AppendMessage {

    // Value
    private String value;
    // Nonce
    private int nonce;
    // Signature
    private byte[] signature;

    // This is a client id
    public AppendMessage(String value, int nonce, byte[] signature) {
        this.value = value;
        this.nonce = nonce;
        this.signature = signature;
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
