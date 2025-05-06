package com.tfg.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat {
    private String id;
    private List<String> userIds;
    private String lastMessage;
    private Timestamp timestamp;

    public Chat() { /* Firestore necesita constructor vacío */ }

    public Chat(String id, List<String> userIds, String lastMessage, Timestamp timestamp) {
        this.id = id;
        this.userIds = userIds;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public List<String> getUserIds() { return userIds; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getTimestamp() { return timestamp; }
}
