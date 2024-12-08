package com.chat.server;

import java.sql.Timestamp;

public class ChatMessage {
    private String sender;
    private String message;
    private Timestamp timestamp;
    private boolean isSystemMessage;

    public ChatMessage(String sender, String message, Timestamp timestamp, boolean isSystemMessage) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.isSystemMessage = isSystemMessage;
    }

    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isSystemMessage() { return isSystemMessage; }
}