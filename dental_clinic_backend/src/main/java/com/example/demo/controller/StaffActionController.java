package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StaffActionDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.StaffActionService;

@RestController
@RequestMapping("/api/staff-actions")
public class StaffActionController {
    private final StaffActionService actionService;

    public StaffActionController(StaffActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping
    public List<StaffActionDto.Response> recent(Principal principal) {
        return actionService.recent(ClinicPrincipal.require(principal));
    }

    @PostMapping("/{actionId}/undo")
    public StaffActionDto.Response undo(@PathVariable Long actionId, Principal principal) {
        return actionService.undo(actionId, ClinicPrincipal.require(principal));
    }
}
