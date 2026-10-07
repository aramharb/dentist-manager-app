package com.example.demo.controller;

public class LoginResponse {

    private String role;
    private Long userId;
    private String username;
    private String fullName;
    private String token;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(String role, String message) {
        this.role = role;
        this.message = message;
    }

    public LoginResponse(Long userId, String username, String fullName, String role, String token, String message) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
