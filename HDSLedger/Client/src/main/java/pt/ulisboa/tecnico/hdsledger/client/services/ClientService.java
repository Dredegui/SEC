package pt.ulisboa.tecnico.hdsledger.client.services;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
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

        link.broadcast(consensusMessage);
    }

}