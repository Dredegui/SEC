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

    // Método para enviar uma solicitação de anexação ao serviço de blockchain
    public void append(String data) {

        ConsensusMessage consensusMessage = new ConsensusMessage("client", Message.Type.APPEND);

        consensusMessage.setMessage(new AppendMessage(data).toJson());

        link.broadcast(consensusMessage);
    }

    // Método para receber a resposta do serviço de blockchain
    /* public String receive() {
        Message jsonResponse = link.receive();

        return jsonResponse;
    } */
}