package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {}

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private String publicKey;

    private boolean isClient;

    public boolean isLeader() {
        return isLeader;
    }

    public boolean isLeader(int round) {
        return Integer.toString(round).equals(id);
    }

    public boolean isClient() {
        return isClient;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void updateLeader(int round) {
        // convert round to string
        String sRound = Integer.toString(round);
        if (sRound.equals(id)) {
            isLeader = true;
        } else {
            isLeader = false;
        }
    }


}
