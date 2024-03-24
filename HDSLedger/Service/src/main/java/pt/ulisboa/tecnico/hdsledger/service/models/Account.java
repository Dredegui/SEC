package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private double balance;

    public Account() {
        this.balance = 1000;
    }
    
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean hasEnoughBalance(double amount) {
        return this.balance >= amount;
    }

    public double updateBalance(double amount) {
        if (!hasEnoughBalance(amount)) {
            return -1;
        }
        this.balance += amount;
        return this.balance;
    }
}

