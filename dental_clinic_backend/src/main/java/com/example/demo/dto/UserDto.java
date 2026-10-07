package com.example.demo.dto;

public class UserDto {
    public record Response(Long id, String username, String fullName, String role, boolean online) {}
}
