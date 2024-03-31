package pt.ulisboa.tecnico.hdsledger.service.models;

public class Account {
    private double authorizedBalance;
    private double bookBalance;
    private String publicKeyHash;


    public Account(String publicKeyHash) {
        this.authorizedBalance = 1000;
        this.bookBalance = 1000;
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
        if (authorizedBalance < 0) {
            throw new IllegalArgumentException("Authorized balance cannot be negative");
        }
        this.authorizedBalance = authorizedBalance;
    }

    public boolean hasEnoughAuthorizedBalance(double amount) {
        return this.authorizedBalance >= amount;
    }

    public void updateAuthorizedBalance(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.authorizedBalance -= amount;
    }

    public void updateBookBalance(double amount) {
        if (this.bookBalance + amount < 0) {
            throw new IllegalArgumentException("Book balance cannot be negative");
        }
        this.bookBalance += amount;
    }

    public double getBookBalance() {
        return bookBalance;
    }

    public void setBookBalance(double bookBalance) {
        if (bookBalance < 0) {
            throw new IllegalArgumentException("Book balance cannot be negative");
        }
        this.bookBalance = bookBalance;
    }
}

