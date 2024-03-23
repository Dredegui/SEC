package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CheckBalanceMessage {

    double balance;

    private String publicKey;

    // Constructor used by client
    public CheckBalanceMessage() {
        this.balance = -1;
    }

    // Constructor used by server
    public CheckBalanceMessage(double balance, String publicKey){
        this.balance = balance;
        this.publicKey = publicKey;
    }

    public double getBalance(){
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getPublicKey() {
        return this.publicKey;
    }
    
    public String toJson() {
        return new Gson().toJson(this);
    }
}
