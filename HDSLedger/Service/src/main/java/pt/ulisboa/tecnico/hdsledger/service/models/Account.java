package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private double autorizedBalance;
    private double contablisticBalance;
    private String publicKeyHash;


    public Account(String publicKeyHash) {
        this.autorizedBalance = 1000;
        this.contablisticBalance = 1000;
        this.publicKeyHash = publicKeyHash;
    }

    public String getPublicKeyHash() {
        return publicKeyHash;
    }

    public void setPublicKeyHash(String publicKeyHash) {
        this.publicKeyHash = publicKeyHash;
    }
    
    public double getAutorizedBalance() {
        return autorizedBalance;
    }

    public void setAutorizedBalance(double autorizedBalance) {
        this.autorizedBalance = autorizedBalance;
    }

    public boolean hasEnoughAutorizedBalance(double amount) {
        return this.autorizedBalance >= amount;
    }

    public double updateBalance(double amount) {
        if (!hasEnoughAutorizedBalance(amount)) {
            return -1;
        }
        this.autorizedBalance += amount;
        return this.autorizedBalance;
    }

    public double getContablisticBalance() {
        return contablisticBalance;
    }

    public void setContablisticBalance(double contablisticBalance) {
        this.contablisticBalance = contablisticBalance;
    }
}

