package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class RoundChangeMessage {

    // Value
    private String preparedRound;
    private String preparedValue;

    public RoundChangeMessage(String preparedRound, String preparedValue) {
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
    }

    public String getPreparedRound() {
        return preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}

