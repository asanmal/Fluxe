package com.tfg.models;

public class Comment {
    private String id;
    private String authorUid;
    private String text;
    private long   timestamp;

    public Comment() {}

    public Comment(String id, String authorUid, String text, long timestamp) {
        this.id = id;
        this.authorUid = authorUid;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
