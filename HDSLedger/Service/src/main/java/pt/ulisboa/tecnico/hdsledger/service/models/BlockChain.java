package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.hdsledger.utilities.Append;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

public class BlockChain {
    private List<Block> chain;
    private List<Transaction> currentTransactions;
    private List<Append> listOfAppends;
    private int numberOfTransactionsPerBlock = 1;
    
    public BlockChain() {
        chain = new ArrayList<>();
        currentTransactions = new ArrayList<>();
        listOfAppends = new ArrayList<>();
        // initialize a byte[] with 0
        byte[] previousHash = null;
        createBlock(previousHash);
    }
    
    public Block createBlock(byte[] previousHash) {
        Block block = new Block(currentTransactions, previousHash, 0, System.currentTimeMillis());
        this.chain.add(block);
        this.currentTransactions = new ArrayList<>();
        this.listOfAppends = new ArrayList<>();
        return block;
    }
    
    public void addAppend(String clientId, String value, byte[] senderSignature, int nonce) {
        listOfAppends.add(new Append(clientId, value, senderSignature, nonce));
    }
    
    public void addTransaction(String sender, String receiver, double amount, byte[] senderSignature, int nonce) {
        currentTransactions.add(new Transaction(sender, receiver, amount, senderSignature, nonce));
    }

    public List<Block> getChain() {
        return chain;
    }

    public List<Transaction> getCurrentTransactions() {
        return currentTransactions;
    }

    public List<Append> getListOfAppends() {
        return listOfAppends;
    }

    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    public boolean isReadyToProcessTransactions() {
        return currentTransactions.size() >= numberOfTransactionsPerBlock;
    }


    @Override
    public String toString() {
        return "BlockChain{" +
                "chain=" + chain +
                ", currentTransactions=" + currentTransactions +
                '}';
    }
}
