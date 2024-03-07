package pt.ulisboa.tecnico.hdsledger.service.services;

import org.mockito.Mockito;
import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

public class NodeServiceTest {

    private NodeService nodeService;
    private Link mockLink;
    private ProcessConfig mockConfig;
    private ProcessConfig mockLeaderConfig;
    private ProcessConfig[] mockNodeConfigs;

    @Before
    public void setUp() {
        // Setup other mocks and configure mock behaviors as needed
    }
/*
    @Test
    public void testByzantineLeaderCase1() {
        // Node 1 will be byzantine and only deliver messages to node 2
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
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
            Link linkToNodes = new Link(nodeConfig, private_key_path, nodeConfig.getPort(), nodeConfigs,
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
            NodeService nodeService = new NodeService(linkToNodes, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus("ola");
        }
        // sleep for 3 seconds
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // get leader from nodeServices 2
        assertEquals(true, nodeServices.get("2").getConfig().isLeader());
        // get ledger from all nodeServices except 1 (it's byzantine)
        for (int i = 2; i <= 4; i++) {
            assertEquals("ola", nodeServices.get(Integer.toString(i)).getLedger().get(0));
        }       
    } */
    @Test
    public void testRoundResetWhenNewConsensusStarts() {
        // Node 1 will be byzantine and only deliver messages to node 2
        // Simulate node behaviour
        // Create configuration instances and save node services to a list
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
            Link linkToNodes = new Link(nodeConfig, private_key_path, nodeConfig.getPort(), nodeConfigs,
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
            NodeService nodeService = new NodeService(linkToNodes, nodeConfig, leaderConfig,
                nodeConfigs);
            nodeServices.put(id, nodeService);
            nodeService.listen();
        }
        for (int i = 1; i <= 4; i++) {
            nodeServices.get(Integer.toString(i)).startConsensus("hello");
        }
        System.out.println("Sleeping for 4 seconds");
        // sleep for 3 seconds
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopped sleeping");
        
        // New lider is now 2
        for (int i = 2; i <= 4; i++) {
            // For testing effects of new consensus starting, we will not start consensus on node 1
            // Because it's byzantine and it didn't commit the last consensus because it only send messages to node 2
            nodeServices.get(Integer.toString(i)).startConsensus("hello");
        }

        assertEquals(2, nodeServices.get("3").getConsensusInstance());
        assertEquals(1, nodeServices.get("3").getConsensusInstanceRound(2));
    
    }

    // Additional test cases...
}
