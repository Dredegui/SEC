package pt.ulisboa.tecnico.hdsledger.client.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;

public class MessageBucket {

    // Quorum size
    private final int quorumSize;
    private int f;

    // List of CheckBalanceMessages
    private List<CheckBalanceMessage> balanceMessages = new CopyOnWriteArrayList<>();

    private Map<Integer, List<ConfirmationMessage>> transferConfirmationMessages = new HashMap<>();

    // List of ConfirmationMessages for appends
    private List<ConfirmationMessage> appendConfirmationMessages = new CopyOnWriteArrayList<>();
    
    public MessageBucket(int nodeCount) {
        f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    public void addCheckBalanceMessage(CheckBalanceMessage message) {
        balanceMessages.add(message);
    }

    public void clearCheckBalanceMessages() {
        balanceMessages.clear();
    }

    public void addTransferConfirmationMessage(ConfirmationMessage message) {
        if (!transferConfirmationMessages.containsKey(message.getNonce())) {
            transferConfirmationMessages.put(message.getNonce(), new CopyOnWriteArrayList<>());
        }
        transferConfirmationMessages.get(message.getNonce()).add(message);
    }

    public void addAppendConfirmationMessage(ConfirmationMessage message) {
        appendConfirmationMessages.add(message);
    }

    public void clearAppendConfirmationMessage() {
        appendConfirmationMessages.clear();
    }

    public Optional<String> hasValidCheckBalanceQuorum() {
        Map<String, Integer> balanceFrequency = new HashMap<>();

        // Process the list of CheckBalanceMessages
        for (CheckBalanceMessage message : balanceMessages) {
            Double authorizedBalance = message.getAutorizedBalance();
            Double contabilisticBalance = message.getContablisticBalance();

            // Create a key for the balance pair
            String key = authorizedBalance + ":" + contabilisticBalance;

            // Increment the frequency of the balance pair
            balanceFrequency.put(key, balanceFrequency.getOrDefault(key, 0) + 1);
        }

        // Determine quorum for balance
        Optional<String> balanceQuorum = balanceFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() >= quorumSize)
            .map(Map.Entry::getKey)
            .findFirst();


        if (balanceQuorum.isPresent() ) {
            return balanceQuorum;
        }

        // If either quorum is not present, return an empty Optional
        return Optional.empty();
    }

    public Optional<Integer> hasValidTransferConfirmationQuorom(int nonce)  {
        Map<Integer,Integer> transferLedgerFrequency = new HashMap<>();
        List<ConfirmationMessage> transferMessages = this.transferConfirmationMessages.get(nonce);
        for (ConfirmationMessage message : transferMessages) {
            int ledgerMessageLocation = message.getLedgerMessageLocation();

            transferLedgerFrequency.put(ledgerMessageLocation, transferLedgerFrequency.getOrDefault(ledgerMessageLocation, 0) + 1);
        }

        Optional<Integer> transferLedgerQuorum = transferLedgerFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() >= quorumSize)
            .map(Map.Entry::getKey)
            .findFirst();

        if (transferLedgerQuorum.isPresent()) {
            return transferLedgerQuorum;
        }

        return Optional.empty();
    }

    public Optional<String> hasValidAppendConfirmationQuorom()  {
        Map<String,Integer> appendLedgerFrequency = new HashMap<>();

        for (ConfirmationMessage message : appendConfirmationMessages) {
            int ledgerMessageLocation = message.getLedgerMessageLocation();
            int nonce = message.getNonce();
            String key= ledgerMessageLocation + ":" + nonce;

            appendLedgerFrequency.put(key, appendLedgerFrequency.getOrDefault(key, 0) + 1);
        }

        Optional<String> appendLedgerQuorum = appendLedgerFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() >= quorumSize)
            .map(Map.Entry::getKey)
            .findFirst();

        if (appendLedgerQuorum.isPresent()) {
            return appendLedgerQuorum;
        }

        return Optional.empty();
    }
}
