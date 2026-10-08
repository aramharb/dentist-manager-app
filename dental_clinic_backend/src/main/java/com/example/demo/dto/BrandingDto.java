package com.example.demo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BrandingDto {
    /** Identity of a cabinet as shown to clients. {@code imageVersion} changes with the images (cache busting). */
    public record Response(Long cabinetId, String name, String ownerName, String tagline, String primaryColor,
            String address, String phoneNumber, String email, boolean hasLogo, boolean hasCover, long imageVersion) {}

    public record Request(
            @Size(max = 160) String ownerName,
            @Size(max = 200) String tagline,
            @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "Color must look like #1689E8.") String primaryColor,
            @Size(max = 255) String address,
            @Size(max = 30) String phoneNumber,
            @Size(max = 150) String email) {}
}
