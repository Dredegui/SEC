package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.List;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class PrePrepareMessage {
    
    // Value
    private String value;

    private List<Transaction> currentTransactions;

    // Constructor for PrePrepareMessage of Transactions
    public PrePrepareMessage(List<Transaction> currentTransactions) {
        this.currentTransactions = currentTransactions;
    }

    public PrePrepareMessage(String value) {
        this.value = value;
    }

    public List<Transaction> getTransactions() {
        return currentTransactions;
    }

    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
