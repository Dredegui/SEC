package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {
    private List<Block> chain;
    private List<Transaction> currentTransactions;
    
    public BlockChain() {
        chain = new ArrayList<>();
        currentTransactions = new ArrayList<>();
        createBlock("0");
    }
    
    public Block createBlock(String previousHash) {
        Block block = new Block(currentTransactions, previousHash, 0, System.currentTimeMillis());
        this.chain.add(block);
        this.currentTransactions = new ArrayList<>();
        return block;
    }
    
    
    public void addTransaction(String sender, String receiver, double amount) {
        currentTransactions.add(new Transaction(sender, receiver, amount));
    }

    public List<Block> getChain() {
        return chain;
    }

    public List<Transaction> getCurrentTransactions() {
        return currentTransactions;
    }

    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }


    @Override
    public String toString() {
        return "BlockChain{" +
                "chain=" + chain +
                ", currentTransactions=" + currentTransactions +
                '}';
    }
}
