package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;
import java.security.SecureRandom;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.CheckBalanceMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.Link;


public class ClientService {

    private Link link;
    private static SecureRandom random;

    public ClientService(Link link) {
        this.link = link;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void append(String data, String id) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.APPEND);

        consensusMessage.setMessage(new AppendMessage(data, random.nextInt()).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");
        
        this.listenConfirmation();

    }

    public void check_balance(String id) {

        ConsensusMessage consensusMessage = new ConsensusMessage(id, Message.Type.CHECK_BALANCE);

        consensusMessage.setMessage(new CheckBalanceMessage().toJson());

        this.link.broadcast(consensusMessage);
        
        this.listenBalance();

    }

    public void listenConfirmation() {
        
        try {
            boolean listen = true;
            while (listen) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CONFIRMATION) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    ConfirmationMessage confirmationMessage = consensusMessage.deserializeConfirmationMessage();
                    System.out.println("Message appended to the chain successfully in position: " + confirmationMessage.getLedgerMessageLocation());
                    listen = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
    }

    public void listenBalance() {
        
        try {
            boolean listen = true;
            while (listen) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CHECK_BALANCE) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    CheckBalanceMessage checkBalanceMessage = consensusMessage.deserializeCheckBalanceMessage();
                    System.out.println("Your balance is: " + checkBalanceMessage.getBalance());
                    listen = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        
    }

}