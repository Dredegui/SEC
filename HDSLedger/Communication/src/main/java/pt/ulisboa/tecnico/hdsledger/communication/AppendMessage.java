package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class AppendMessage extends Message{

    // Value
    private String value;

    public AppendMessage(String senderId, Type type, String value) {
        super(senderId, type);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
