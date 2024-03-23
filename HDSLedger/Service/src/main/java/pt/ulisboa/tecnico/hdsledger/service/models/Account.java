package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private String publicKey;
    private double balance;

    public Account(String publicKey, double balance) {
        this.publicKey = publicKey;
        this.balance = balance;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}

