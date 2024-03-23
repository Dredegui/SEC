package pt.ulisboa.tecnico.hdsledger.service;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

public class Node {

    private static final CustomLogger LOGGER = new CustomLogger(Node.class.getName());
    // Hardcoded path to files
    private static String nodesConfigPath = "src/main/resources/";
    private static String private_key_path = "src/main/resources/";
    private static String account1_public_key_path = "src/main/resources/publicKeys/clientPublic.key";
    private static String account2_public_key_path = "src/main/resources/publicKeys/clientPublic1.key";

    public static void main(String[] args) {

        try {
            // Command line arguments
            String id = args[0];
            private_key_path += args[1];
            nodesConfigPath += args[2];

            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                    nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                    nodeConfig.isLeader(), nodeConfig.getPublicKey()));

            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, private_key_path, nodeConfig.getPort(), nodeConfigs,
                    ConsensusMessage.class);

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, nodeConfig, leaderConfig,
                    nodeConfigs);

            nodeService.addAccount(new Account("-----BEGIN PUBLIC KEY-----\r\n" + //
                                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzqyzfjJQXPu0RHMgMtEZ\r\n" + //
                                "JM70DJe7uXAn57h0wxJ3JVoUhmZOyr5YrxgW0METdM9d+WcD8kLG6TiCeAYynjep\r\n" + //
                                "l064JoF6VnQEAftrPeMv4cxoPas9d4YP4OGEGunbGVpzP0jMLCVm53dvjLwhmRCF\r\n" + //
                                "Qpo0dG9yNYLTOmTNQBtydcRrVIHJ7oTF8N6ol5RO30gdTCTAuL+MQEOudJasnH9y\r\n" + //
                                "1qhSg3K/G48SImZNoxliUBTLH9xLQe6Wppi7QQNrEl4BLzZD6Rm9AoVapV68CK21\r\n" + //
                                "r4bxbbRm2HJ1CtOSjjCpC3MFqQPREg0tEE+I+Hao9sJbBPeIcbFqbMz/nHMWz/0d\r\n" + //
                                "hwIDAQAB\r\n" + //
                                "-----END PUBLIC KEY-----"));
            nodeService.addAccount(new Account("-----BEGIN PUBLIC KEY-----\r\n" + //
                                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtWKHjeTSfo7S+GFkodui\r\n" + //
                                "XA5o27RtwI/EA/QO9Cp7tBxb5OB2fsbbe4a+G+AORVBGaJy+Z+36mU1qvsr9X1tg\r\n" + //
                                "oLt7rLhaFhU1PkKo3hwPk4VxmNeRHpj568g86W2czMBw5b+8eNB/Mv2JtklEIqqv\r\n" + //
                                "rcKDBS9YQQ2uV6dsxqoi9YEBM4O20nI3lOHbj/Ynv1R6GyIC3SRbxY3GWZOYF2vt\r\n" + //
                                "AK25HPr2uUWEsM+hOGaVQTZ6MLSVrhd306AoAnSUzH/a4B3sZ3rX1yPGm1t9xzw8\r\n" + //
                                "fbNpQUslwLUKW/nGwySWpoB4rt3FNUxCJPYqlN6/9bttXkyzd4qL6DqMUzgM8jVB\r\n" + //
                                "jQIDAQAB\r\n" + //
                                "-----END PUBLIC KEY-----"));

            nodeService.listen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
