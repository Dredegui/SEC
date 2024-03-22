package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CheckBalanceMessage {

    int balance;

    // Constructor used by client
    public CheckBalanceMessage() {
        this.balance = -1;
    }

    // Constructor used by server
    public CheckBalanceMessage(int balance){
        this.balance = balance;
    }

    public int getBalance(){
        return this.balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }
    
    public String toJson() {
        return new Gson().toJson(this);
    }
}
