package com.tfg.models;

public class User {
    private String id;
    private String username;
    private String profile_picture;
    private String status;

    public User(String id, String username, String profile_picture, String status) {
        this.id = id;
        this.username = username;
        this.profile_picture = profile_picture;
        this.status = status;
    }

    public User(){

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfile_picture() {
        return profile_picture;
    }

    public void setProfile_picture(String profile_picture) {
        this.profile_picture = profile_picture;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
