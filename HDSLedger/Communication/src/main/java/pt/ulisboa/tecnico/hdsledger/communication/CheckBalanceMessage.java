package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CheckBalanceMessage {

    double balance;


    // Constructor used by client
    public CheckBalanceMessage() {
        this.balance = -1;
    }

    // Constructor used by server
    public CheckBalanceMessage(double balance){
        this.balance = balance;
    }

    public double getBalance(){
        return this.balance;
    }



    public void setBalance(double balance) {
        this.balance = balance;
    }


    public String toJson() {
        return new Gson().toJson(this);
    }
}
