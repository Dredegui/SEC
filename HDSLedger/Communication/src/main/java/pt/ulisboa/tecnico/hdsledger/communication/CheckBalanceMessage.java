package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CheckBalanceMessage {

    double balance;

    private String publicKey;

    // Constructor used by client
    public CheckBalanceMessage(String publicKey) {
        this.balance = -1;
        this.publicKey = publicKey;
    }

    // Constructor used by server
    public CheckBalanceMessage(double balance){
        this.balance = balance;
    }

    public double getBalance(){
        return this.balance;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
