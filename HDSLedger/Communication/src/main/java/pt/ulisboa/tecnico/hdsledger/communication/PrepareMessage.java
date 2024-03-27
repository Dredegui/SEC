package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.List;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

public class PrepareMessage {
    
    // Value
    private String value;

    private List<Transaction> currentTransactions;

    // Constructor for Transaction PrepareMessage
    public PrepareMessage(List<Transaction> currentTransactions) {
        this.currentTransactions = currentTransactions;
    }

    public PrepareMessage(String value) {
        this.value = value;
    }

    public List<Transaction> getCurrentTransactions() {
        return currentTransactions;
    }

    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
