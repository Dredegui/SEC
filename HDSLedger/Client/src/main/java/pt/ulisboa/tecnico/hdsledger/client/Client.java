package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.communication.Link;

import java.util.Scanner;
import java.util.Arrays;

public class Client {

    private static String nodesConfigPath = "src/main/resources/regular_config.json";
    private static String private_key_path = "src/main/resources/";

    private ClientService clientService;

    public Client(ClientService clientService) {
        this.clientService = clientService;
        
    }
    
    private void appendMessage(String message, String id) {
        clientService.append(message, id);
    }

    private void check_balance(String id, String publicKey){
        clientService.check_balance(id, publicKey);
    }

    private void transfer(String destinationId, int amount) {
        System.out.println("Transfering " + amount + " to " + destinationId);
        //clientService.transfer(destinationId,amount);
    }

    public void cli(String id, String publicKey) {
            Scanner scanner = new Scanner(System.in);
            boolean running = true;
    
            while (running) {
                System.out.println("Choose an option:");
                System.out.println("1. Append message to the chain");
                System.out.println("2. Check account balance");
                System.out.println("3. Transfer funds");
                System.out.println("5. Exit");
    
                int choice = scanner.nextInt();
                scanner.nextLine();
    
                switch (choice) {
                    case 1:
                        System.out.print("Enter the message to append to the chain: ");
                        String message = scanner.nextLine();
                        appendMessage(message,id);
                        break;
                    case 2:
                        check_balance(id, publicKey);
                        break;
                    case 3:
                        System.out.print("Enter the destination: ");
                        String destinationId = scanner.nextLine();
                        System.out.print("Enter the amount to transfer: ");
                        int amount = scanner.nextInt();
                        transfer(destinationId,amount);
                        break;
                    case 5:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
    
            // Close the scanner
            scanner.close();
        }

    public static void main(String[] args) {

        // Command line arguments
        String id = args[0];
        private_key_path += args[1];

        // Create configuration instances
        ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
        ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();


        // Abstraction to send and receive messages
        Link linkToNodes = new Link(nodeConfig, private_key_path, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
        
        ClientService clientService = new ClientService(linkToNodes, private_key_path);
        Client client = new Client(clientService);

        // Start CLI
        client.cli(id, nodeConfig.getPublicKey());
    }
}
