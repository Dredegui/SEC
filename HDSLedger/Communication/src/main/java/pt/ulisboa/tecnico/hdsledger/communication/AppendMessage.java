package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class AppendMessage {

    // Value
    private String value;

    // This is a client id
    public AppendMessage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
