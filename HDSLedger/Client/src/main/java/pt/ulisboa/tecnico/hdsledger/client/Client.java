package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.Link;

import java.util.Arrays;

public class Client {

    //private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());
    // Hardcoded path to files
    private static String private_key_path = "/home/miguel/SEC/Projeto/SEC/HDSLedger/Service/src/main/resources/privateKeys/client.key";
    private static String nodesConfigPath = "/home/miguel/SEC/Projeto/SEC/HDSLedger/Service/src/main/resources/regular_config.json";
    
    private ClientService clientService;

    public Client(ClientService clientService) {
        this.clientService = clientService;
    }
    
    public void append(String data) {
        clientService.append(data);
    }

    /* public String receive() {
        return clientService.receive();
    } */

    public static void main(String[] args) {

        // Command line arguments
        String id = args[0];

        // Create configuration instances
        ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
        ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

        // Abstraction to send and receive messages
        Link linkToNodes = new Link(nodeConfig, private_key_path, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
        
        ClientService clientService = new ClientService(linkToNodes);
        Client client = new Client(clientService);

        client.append("Bitcoin");
        System.out.println("Data sent to blockchain");
        /* String confirmation = client.receive();
        System.out.println(confirmation); */
    }
}
