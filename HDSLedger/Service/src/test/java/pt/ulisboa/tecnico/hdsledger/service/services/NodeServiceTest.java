package pt.ulisboa.tecnico.hdsledger.service.services;

import org.mockito.Mockito;
import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.TransferMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.Append;
import pt.ulisboa.tecnico.hdsledger.utilities.CryptSignature;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.utilities.Transaction;

@Execution(ExecutionMode.SAME_THREAD)
public class NodeServiceTest {

    @Test
    public void testByzantineLeader() {
        // Node 1 will be byzantine and only deliver messages to node 2
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        System.out.println("-------------------------------------------------");
        System.out.println("---------------testByzantineLeader---------------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            if (i == 1) {
                // Spy linkToNodes 
                linkToNodes = Mockito.spy(linkToNodes);
                Mockito.doAnswer(invocation -> {
                    int port = invocation.getArgument(1);
                    if (port != 3002) {
                        return null;
                    }
                    return invocation.callRealMethod();
                }).when(linkToNodes).unreliableSend(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
            }

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        String value = "ola";
        int nonce = 1;
        byte[] data = (value + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        Append append = new Append("client", value, signature, nonce);
        List<Append> listOfAppends = new ArrayList<>();
        listOfAppends.add(append);
        String actualValue = nodeServices.get("1").serializeListOfAppends(listOfAppends);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // sleep for 3 seconds
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // get leader from nodeServices 2
        System.out.println("Leader is 2? " + nodeServices.get("3").isLeader("2"));
        assertEquals(true, nodeServices.get("2").getConfig().isLeader());
        // get ledger from all nodeServices except 1 (it's byzantine)
        for (int i = 2; i <= 4; i++) {
            assertEquals(actualValue, nodeServices.get(Integer.toString(i)).getLedger().get(0));
        }
        value = "hello";
        nonce = 2;
        data = (value + nonce).getBytes();
        signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        Append append2 = new Append("client", value, signature, nonce);
        listOfAppends.clear();
        listOfAppends.add(append2);
        actualValue = nodeServices.get("1").serializeListOfAppends(listOfAppends);
        // New lider is now 2
        for (int i = 2; i <= 4; i++) {
            // For testing effects of new consensus starting, we will not start consensus on node 1
            // Because it's byzantine and it didn't commit the last consensus because it only send messages to node 2
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }

        assertEquals(2, nodeServices.get("3").getConsensusInstance());
        assertEquals(1, nodeServices.get("3").getConsensusInstanceRound(2));
        System.out.println("Leader is 1? " + nodeServices.get("3").isLeader("1"));
        assertEquals(true, nodeServices.get("3").isLeader("1"));
        // for all nodeServices close their sockets
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        // Close any existing threads
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    } 

        
    @Test
    public void testByzantineNode() {
        // Node 4 will be byzantine and we do not send messages
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("----------------testByzantineNode----------------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            if (i == 4) {
                // Spy linkToNodes 
                linkToNodes = Mockito.spy(linkToNodes);
                Mockito.doAnswer(invocation -> {
                    return null;
                }).when(linkToNodes).unreliableSend(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
            }

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        String value = "ola";
        int nonce = 1;
        byte[] data = (value + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        Append append = new Append("client", value, signature, nonce);
        List<Append> listOfAppends = new ArrayList<>();
        listOfAppends.add(append);
        String actualValue = nodeServices.get("1").serializeListOfAppends(listOfAppends);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // sleep for 7 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // get ledger from all nodeServices except 4 (it's byzantine)
        for (int i = 1; i <= 3; i++) {
            assertEquals(actualValue, nodeServices.get(Integer.toString(i)).getLedger().get(0));
        }
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }
    
    @Test
    public void testTwoClients() {
        // Node 4 will be byzantine and we do not send messages
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("------------------testTwoClients-----------------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        String valueC1 = "ola";
        int nonceC1 = 1;
        byte[] dataC1 = (valueC1 + nonceC1).getBytes();
        byte[] signatureC1 = CryptSignature.sign(dataC1, "src/main/resources/privateKeys/rk_client.key");
        String valueC2 = "adeus";
        Append append = new Append("client", valueC1, signatureC1, nonceC1);
        List<Append> listOfAppends = new ArrayList<>();
        listOfAppends.add(append);
        String actualValue = nodeServices.get("1").serializeListOfAppends(listOfAppends);
        int nonceC2 = 1;
        byte[] dataC2 = (valueC2 + nonceC2).getBytes();
        byte[] signatureC2 = CryptSignature.sign(dataC2, "src/main/resources/privateKeys/rk_client1.key");
        Append append2 = new Append("client1", valueC2, signatureC2, nonceC2);
        List<Append> listOfAppends2 = new ArrayList<>();
        listOfAppends2.add(append2);
        String actualValue2 = nodeServices.get("1").serializeListOfAppends(listOfAppends2);
        for (int i = 1; i <= 3; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        nodeServices.get(Integer.toString(4)).startConsensus(actualValue2);
        for (int i = 1; i <= 3; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue2);
        }
        nodeServices.get(Integer.toString(4)).startConsensus(actualValue);
        // sleep for 7 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // get ledger from all nodeServices except 4 (it's byzantine)
        for (int i = 1; i <= 4; i++) {
            assertEquals(actualValue, nodeServices.get(Integer.toString(i)).getLedger().get(0));
        }
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void transactionNonByzantine() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("---------------transactionNonByzantine-----------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will transfer money from client to client1
        NodeService nodeService = nodeServices.get("1");
        double amount = 100;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        double clientBalance = nodeService.getAccount(sourcePublicKeyHash).getBookBalance();
        double client1Balance = nodeService.getAccount(destinyPublicKeyHash).getBookBalance();
        int nonce = 1;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        List<Transaction> listOfTransactions = new ArrayList<>();
        Transaction transaction = new Transaction(sourcePublicKeyHash, destinyPublicKeyHash, amount, signature, nonce);
        listOfTransactions.add(transaction);
        String actualValue = nodeService.serializeCurrentTransactions(listOfTransactions);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // sleep for 6 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Transfer should be successful, so balances should be updated
        assertEquals(clientBalance - (amount + nodeService.getGas()), nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance + amount, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void replayTransactionAttack() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("---------------replayTransactionAttack-----------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        NodeService nodeService = nodeServices.get("1");
        // Client will transfer money from client to client1
        double amount = 100;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        double clientBalance = nodeService.getAccount(sourcePublicKeyHash).getBookBalance();
        double client1Balance = nodeService.getAccount(destinyPublicKeyHash).getBookBalance();
        int nonce = 1;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        List<Transaction> listOfTransactions = new ArrayList<>();
        Transaction transaction = new Transaction(sourcePublicKeyHash, destinyPublicKeyHash, amount, signature, nonce);
        listOfTransactions.add(transaction);
        String actualValue = nodeService.serializeCurrentTransactions(listOfTransactions);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // sleep for 6 seconds  
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int gas = nodeService.getGas();
        // Transfer should be successful, so balances should be updated
        assertEquals(clientBalance - (amount + gas), nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance + amount, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        // Replay attack
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // sleep for 6 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Transfer should not be successful, so balances should remain the same
        assertEquals(clientBalance - (amount + gas), nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance + amount, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void byzantineClientRandomSignature() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("----------byzantineClientRandomSignature---------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will attempt to transfer money from client1 to himself
        double amount = 100;
        int nonce = 1;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        double clientBalance = nodeServices.get("1").getAccount(destinyPublicKeyHash).getBookBalance();
        double client1Balance = nodeServices.get("1").getAccount(sourcePublicKeyHash).getBookBalance();
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        //byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        // random number generator
        int random = (int) (Math.random() * 4 + 1);
        byte[] randomSignature = new byte[256];
        // make random signature with 256 bytes
        for (int i = 0; i < 256; i++) {
            randomSignature[i] = (byte) random;
        }
        List<Transaction> listOfTransactions = new ArrayList<>();
        Transaction transaction = new Transaction(sourcePublicKeyHash, destinyPublicKeyHash, amount, randomSignature, nonce);
        listOfTransactions.add(transaction);
        String actualValue = nodeServices.get("1").serializeCurrentTransactions(listOfTransactions);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // Transfer should not be successful, so balances should remain the same
        assertEquals(clientBalance, nodeServices.get("1").getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance, nodeServices.get("1").getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }


    @Test
    public void byzantineClientRealSignature() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-------------------------------------------------");
        System.out.println("----------byzantineClientRealSignature---------");
        System.out.println("-------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will attempt to transfer money from client1 to himself
        double amount = 100;
        int nonce = 1;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        double clientBalance = nodeServices.get("1").getAccount(destinyPublicKeyHash).getBookBalance();
        double client1Balance = nodeServices.get("1").getAccount(sourcePublicKeyHash).getBookBalance();
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");
        List<Transaction> listOfTransactions = new ArrayList<>();
        Transaction transaction = new Transaction(sourcePublicKeyHash, destinyPublicKeyHash, amount, signature, nonce);
        listOfTransactions.add(transaction);
        String actualValue = nodeServices.get("1").serializeCurrentTransactions(listOfTransactions);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus(actualValue);
        }
        // Transfer should not be successful, so balances should remain the same
        assertEquals(clientBalance, nodeServices.get("1").getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance, nodeServices.get("1").getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void InsufficientBalanceNonByzantine() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("---------------------------------------------------------");
        System.out.println("---------------InsufficientBalanceNonByzantine-----------");
        System.out.println("---------------------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will transfer money from client to client1
        NodeService nodeService = nodeServices.get("1");
        double amount = 2000;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        double clientBalance = nodeService.getAccount(sourcePublicKeyHash).getBookBalance();
        double client1Balance = nodeService.getAccount(destinyPublicKeyHash).getBookBalance();
        double clientAuthorizedBalance = nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance();
        double client1AuthorizedBalance = nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance();
        int nonce = 1;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");

        // Create a Transfer Message
        ConsensusMessage consensusMessage = new ConsensusMessage("client", Message.Type.TRANSFER);
        consensusMessage.setSignature(signature);
        consensusMessage.setMessage(new TransferMessage(sourcePublicKeyHash, destinyPublicKeyHash, amount, nonce).toJson());

        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).uponTransfer(consensusMessage);
        }
        // sleep for 6 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Transfer should not be successful
        assertEquals(clientBalance, nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(clientAuthorizedBalance, nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance(), 0.0001);
        assertEquals(client1AuthorizedBalance, nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance(), 0.0001);
        
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void NegativeBalance() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        
        System.out.println("-----------------------------------------");
        System.out.println("---------------NegativeBalance-----------");
        System.out.println("-----------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will transfer money from client to client1
        NodeService nodeService = nodeServices.get("1");
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        Boolean errorThrown = false;
        try {
            nodeService.getAccount(sourcePublicKeyHash).setBookBalance(-100);
            nodeService.getAccount(sourcePublicKeyHash).setAuthorizedBalance(-100);
        } catch (Exception e) {
            // Should not be possible to have a negative balance
            errorThrown = true;
            double clientAuthorizedBalance = nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance();
            double client1AuthorizedBalance = nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance();
            double clientBalance = nodeService.getAccount(sourcePublicKeyHash).getBookBalance();
            double client1Balance = nodeService.getAccount(destinyPublicKeyHash).getBookBalance();
            assertEquals(clientBalance, nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
            assertEquals(client1Balance, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
            assertEquals(clientAuthorizedBalance, nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance(), 0.0001);
            assertEquals(client1AuthorizedBalance, nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance(), 0.0001);    
        }
        assertEquals(true, errorThrown);
        // sleep for 6 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

    @Test
    public void transferWithNegativeAmount() {
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
        System.out.println("----------------------------------------------");
        System.out.println("-----------transferWithNegativeAmount---------");
        System.out.println("----------------------------------------------");
        HashMap<String, NodeService> nodeServices = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            String id = Integer.toString(i);
            String private_key_path = "src/main/resources/privateKeys/rk_" + id + ".key";
            String nodesConfigPath = "src/main/resources/regular_config.json";
            
            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

            String log = MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}; public key: {4}",
                nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                nodeConfig.isLeader(), nodeConfig.getPublicKey());

            System.out.println(log);
            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                ConsensusMessage.class);
            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, private_key_path, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        // Client will transfer money from client to client1
        NodeService nodeService = nodeServices.get("1");
        double amount = -100;
        String sourcePublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/clientPublic.key"));
        String destinyPublicKeyHash = CryptSignature.hashString(CryptSignature.loadPublicKey("src/main/resources/publicKeys/client1Public.key"));
        double clientBalance = nodeService.getAccount(sourcePublicKeyHash).getBookBalance();
        double client1Balance = nodeService.getAccount(destinyPublicKeyHash).getBookBalance();
        double clientAuthorizedBalance = nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance();
        double client1AuthorizedBalance = nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance();
        int nonce = 1;
        byte[] data = (sourcePublicKeyHash + destinyPublicKeyHash + amount + nonce).getBytes();
        byte[] signature = CryptSignature.sign(data, "src/main/resources/privateKeys/rk_client.key");

        // Create a Transfer Message
        ConsensusMessage consensusMessage = new ConsensusMessage("client", Message.Type.TRANSFER);
        consensusMessage.setSignature(signature);
        consensusMessage.setMessage(new TransferMessage(sourcePublicKeyHash, destinyPublicKeyHash, amount, nonce).toJson());

        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).uponTransfer(consensusMessage);
        }
        // sleep for 6 seconds
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Transfer should not be successful
        assertEquals(clientBalance, nodeService.getAccount(sourcePublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(client1Balance, nodeService.getAccount(destinyPublicKeyHash).getBookBalance(), 0.0001);
        assertEquals(clientAuthorizedBalance, nodeService.getAccount(sourcePublicKeyHash).getAuthorizedBalance(), 0.0001);
        assertEquals(client1AuthorizedBalance, nodeService.getAccount(destinyPublicKeyHash).getAuthorizedBalance(), 0.0001);
        
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).close();
        }
        Thread.getAllStackTraces().keySet().forEach(Thread::interrupt);
    }

}
