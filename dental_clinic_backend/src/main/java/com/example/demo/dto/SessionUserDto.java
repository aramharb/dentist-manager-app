package com.example.demo.dto;

import java.util.List;

public record SessionUserDto(
        Long id,
        String username,
        String fullName,
        String role,
        boolean active,
        List<String> permissions,
        Long cabinetId,
        String cabinetName) {
}
