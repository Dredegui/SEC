package pt.ulisboa.tecnico.hdsledger.utilities;

import com.google.gson.Gson;

public class Append {

    private String clientId;
    private String value;
    private byte[] senderSignature;
    private int nonce;


    public Append(String clientId, String value, byte[] senderSignature, int nonce) {
        this.clientId = clientId;
        this.value = value;
        this.senderSignature = senderSignature;
        this.nonce = nonce;
    }

    public String getCLientId() {
        return clientId;
    }

    public String getValue() {
        return value;
    }

    public byte[] getSenderSignature() {
        return senderSignature;
    }

    public int getNonce() {
        return nonce;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setSenderSignature(byte[] senderSignature) {
        this.senderSignature = senderSignature;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public String toJson() {
        return new Gson().toJson(this);
    } 
    
    
}
