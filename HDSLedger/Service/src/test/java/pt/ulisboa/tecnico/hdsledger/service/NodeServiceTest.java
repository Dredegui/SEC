package pt.ulisboa.tecnico.hdsledger.service;

import org.mockito.Mockito;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

public class NodeServiceTest {

    private NodeService nodeService;
    private Link mockLink;
    private ProcessConfig mockConfig;
    private ProcessConfig mockLeaderConfig;
    private ProcessConfig[] mockNodeConfigs;

    @Before
    public void setUp() {
        mockConfig = Mockito.mock(ProcessConfig.class);
        mockLink = Mockito.mock(Link.class);
        mockLeaderConfig = Mockito.mock(ProcessConfig.class);
        mockNodeConfigs = Mockito.mock(ProcessConfig[].class);
        // Setup other mocks and configure mock behaviors as needed
        nodeService = new NodeService(mockLink, mockConfig, mockLeaderConfig,mockNodeConfigs);
    }

    @Test
    public void testCreateConsensusMessage() {
        // Configuer mocklink to overide unreliableSend and do nothing when port equals 3001
        Mockito.when(mockLink.unreliableSend(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()));

        ConsensusMessage message = nodeService.createConsensusMessage("value", 1, 2);
        assertNotNull(message);
        // Assert message properties as expected
    }

    // Additional test cases...
}
