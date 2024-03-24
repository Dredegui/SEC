package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class CheckBalanceMessage {

    double autorizedBalance;
    double contablisticBalance;


    // Constructor used by client
    public CheckBalanceMessage() {
        this.autorizedBalance = -1;
        this.contablisticBalance = -1;
    }

    // Constructor used by server
    public CheckBalanceMessage(double autorizedBalance, double contablisticBalance) {
        this.autorizedBalance = autorizedBalance;
        this.contablisticBalance = contablisticBalance;

    }

    public double getAutorizedBalance(){
        return this.autorizedBalance;
    }



    public void setAutorizedBalance(double autorizedBalance) {
        this.autorizedBalance = autorizedBalance;
    }

    public double getContablisticBalance(){
        return this.contablisticBalance;
    }

    public void setContablisticBalance(double contablisticBalance) {
        this.contablisticBalance = contablisticBalance;
    }


    public String toJson() {
        return new Gson().toJson(this);
    }
}
