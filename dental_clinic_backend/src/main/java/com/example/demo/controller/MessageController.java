package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MessageDto;
import com.example.demo.service.MessageService;
import com.example.demo.security.ClinicPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public List<MessageDto.ConversationResponse> conversations(Principal principal) {
        return messageService.conversations(userId(principal));
    }

    @PostMapping("/conversations")
    public MessageDto.ConversationResponse create(@Valid @RequestBody MessageDto.CreateConversationRequest request,
            Principal principal) {
        return messageService.create(request, userId(principal));
    }

    @GetMapping("/conversations/{conversationId}")
    public List<MessageDto.MessageResponse> messages(@PathVariable Long conversationId, Principal principal) {
        return messageService.messages(conversationId, userId(principal));
    }

    @PostMapping("/conversations/{conversationId}")
    public MessageDto.MessageResponse send(@PathVariable Long conversationId,
            @Valid @RequestBody MessageDto.SendMessageRequest request, Principal principal) {
        return messageService.send(conversationId, userId(principal), request.body());
    }

    @PostMapping("/conversations/{conversationId}/read")
    public void markRead(@PathVariable Long conversationId, Principal principal) {
        messageService.markRead(conversationId, userId(principal));
    }

    private Long userId(Principal principal) {
        if (principal instanceof ClinicPrincipal clinicPrincipal) {
            return clinicPrincipal.userId();
        }
        throw new IllegalStateException("Authenticated clinic user is required.");
    }
}
