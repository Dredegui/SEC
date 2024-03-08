package pt.ulisboa.tecnico.hdsledger.client.services;

import java.io.IOException;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConfirmationMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.Link;


public class ClientService {

    private Link link;

    public ClientService(Link link) {
        this.link = link;
    }

    public void append(String data) {

        ConsensusMessage consensusMessage = new ConsensusMessage("client", Message.Type.APPEND);

        consensusMessage.setMessage(new AppendMessage(data).toJson());

        this.link.broadcast(consensusMessage);

        System.out.println("Waiting for confirmation...");
        
        this.listenConfirmation();

    }

    public void listenConfirmation() {
        
        try {
            boolean listen = true;
            while (listen) {
                Message message = link.receive();
                if (message.getType() == Message.Type.CONFIRMATION) {
                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                    ConfirmationMessage confirmationMessage = consensusMessage.deserializConfirmationMessage();
                    System.out.println("Message appended to the chain successfully in position: " + confirmationMessage.getLedgerMessageLocation());
                    listen = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        


    }

}