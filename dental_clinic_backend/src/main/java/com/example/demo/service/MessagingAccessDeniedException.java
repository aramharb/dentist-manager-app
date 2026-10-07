package com.example.demo.service;

public class MessagingAccessDeniedException extends RuntimeException {
    public MessagingAccessDeniedException(String message) {
        super(message);
    }
}
