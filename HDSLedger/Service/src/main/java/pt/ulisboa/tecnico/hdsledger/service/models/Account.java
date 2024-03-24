package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private double autorizedBalance;
    private double contablisticBalance;

    public Account() {
        this.autorizedBalance = 1000;
        this.contablisticBalance = 1000;
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

