package pt.ulisboa.tecnico.hdsledger.client.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;

public class MessageBucket {

    // Quorum size
    private final int quorumSize;
    private int f;

    // List of CheckBalanceMessages
    private List<CheckBalanceMessage> balanceMessages = new CopyOnWriteArrayList<>();
    
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

    public Optional<Double[]> hasValidCheckBalanceQuorum() {
        Map<Double, Integer> authorizedFrequency = new HashMap<>();
        Map<Double, Integer> contabilisticFrequency = new HashMap<>();

        // Process the list of CheckBalanceMessages
        for (CheckBalanceMessage message : balanceMessages) {
            Double authorizedBalance = message.getAutorizedBalance();
            Double contabilisticBalance = message.getContablisticBalance();

            authorizedFrequency.put(authorizedBalance, authorizedFrequency.getOrDefault(authorizedBalance, 0) + 1);
            contabilisticFrequency.put(contabilisticBalance, contabilisticFrequency.getOrDefault(contabilisticBalance, 0) + 1);
        }

        // Determine quorum for authorized balance
        Optional<Double> authorizedQuorum = authorizedFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() >= quorumSize)
            .map(Map.Entry::getKey)
            .findFirst();

        // Determine quorum for contabilistic balance
        Optional<Double> contabilisticQuorum = contabilisticFrequency.entrySet().stream()
            .filter(entry -> entry.getValue() >= quorumSize)
            .map(Map.Entry::getKey)
            .findFirst();

        // Return a combined Optional of both balances if both quorums are present
        if (authorizedQuorum.isPresent() && contabilisticQuorum.isPresent()) {
            return Optional.of(new Double[] {authorizedQuorum.get(), contabilisticQuorum.get()});
        }

        // If either quorum is not present, return an empty Optional
        return Optional.empty();
    }
}
