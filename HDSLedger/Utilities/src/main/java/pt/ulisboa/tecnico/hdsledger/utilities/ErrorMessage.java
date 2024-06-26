package pt.ulisboa.tecnico.hdsledger.utilities;

public enum ErrorMessage {
    ConfigFileNotFound("The configuration file is not available at the path supplied"),
    ConfigFileFormat("The configuration file has wrong syntax"),
    NoSuchNode("Can't send a message to a non existing node"),
    SocketSendingError("Error while sending message"),
    ExtractKeyError("Error while extracting key from file"),
    InvalidMac("MAC doesn't match the message content"),
    InvalidSignature("Signature doesn't match the message content"),
    CannotOpenSocket("Error while opening socket");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
