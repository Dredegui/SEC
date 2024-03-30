package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.List;

import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class Block {
    private List<Transaction> transactions;
    private final byte[] previousHash; 
    private final long nonce;
    private final long timestamp;
    
    public Block(List<Transaction> transactions, byte[] previousHash, long nonce, long timestamp) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.nonce = nonce;
        this.timestamp = timestamp;
    }
    
    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public byte[] getPreviousHash() {
        return previousHash;
    }

    public long getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Block{" +
                "transactions=" + transactions +
                ", previousHash='" + previousHash + '\'' +
                ", nonce=" + nonce +
                ", timestamp=" + timestamp +
                '}';
    }
   
}
