package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private double authorizedBalance;
    private double contablisticBalance;
    private String publicKeyHash;


    public Account(String publicKeyHash) {
        this.authorizedBalance = 1000;
        this.contablisticBalance = 1000;
        this.publicKeyHash = publicKeyHash;
    }

    public String getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }
    
    public double getAuthorizedBalance() {
        return authorizedBalance;
    }

    public void setAuthorizedBalance(double authorizedBalance) {
        this.authorizedBalance = authorizedBalance;
    }

    public boolean hasEnoughAuthorizedBalance(double amount) {
        return this.authorizedBalance >= amount;
    }

    public void updateBalance(double amount) {
        this.authorizedBalance += amount;
    }

    public double getContablisticBalance() {
        return contablisticBalance;
    }

    public void setContablisticBalance(double contablisticBalance) {
        this.contablisticBalance = contablisticBalance;
    }
}

