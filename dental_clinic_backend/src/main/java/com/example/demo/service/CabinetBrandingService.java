package com.example.demo.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.BrandingDto;
import com.example.demo.entity.Cabinet;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CabinetRepository;

/** Name, owner, colour and pictures of a cabinet: edited by its manager or the admin, shown to clients. */
@Service
public class CabinetBrandingService {
    public static final long MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    public record Image(String contentType, byte[] data) {}

    private final CabinetRepository cabinetRepository;
    private final JdbcTemplate jdbc;

    public CabinetBrandingService(CabinetRepository cabinetRepository, JdbcTemplate jdbc) {
        this.cabinetRepository = cabinetRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public BrandingDto.Response get(Long cabinetId) {
        return toResponse(find(cabinetId));
    }

    @Transactional
    public BrandingDto.Response update(Long cabinetId, BrandingDto.Request request) {
        Cabinet cabinet = find(cabinetId);
        cabinet.setOwnerName(blankToNull(request.ownerName()));
        cabinet.setTagline(blankToNull(request.tagline()));
        if (request.primaryColor() != null && !request.primaryColor().isBlank()) {
            cabinet.setPrimaryColor(request.primaryColor().toUpperCase());
        }
        cabinet.setAddress(blankToNull(request.address()));
        cabinet.setPhoneNumber(blankToNull(request.phoneNumber()));
        cabinet.setEmail(blankToNull(request.email()));
        return toResponse(cabinetRepository.save(cabinet));
    }

    @Transactional
    public BrandingDto.Response saveImage(Long cabinetId, String kind, byte[] data) {
        find(cabinetId);
        requireKind(kind);
        if (data == null || data.length == 0) throw new IllegalArgumentException("The image is empty.");
        if (data.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("The image must be 2 MB or smaller.");
        String contentType = sniff(data);
        jdbc.update("""
                insert into cabinet_image (cabinet_id, kind, content_type, data, updated_at)
                values (?, ?, ?, ?, now())
                on conflict (cabinet_id, kind) do update
                set content_type = excluded.content_type, data = excluded.data, updated_at = now()
                """, cabinetId, kind, contentType, data);
        return get(cabinetId);
    }

    @Transactional
    public BrandingDto.Response deleteImage(Long cabinetId, String kind) {
        find(cabinetId);
        requireKind(kind);
        jdbc.update("delete from cabinet_image where cabinet_id = ? and kind = ?", cabinetId, kind);
        return get(cabinetId);
    }

    @Transactional(readOnly = true)
    public Optional<Image> image(Long cabinetId, String kind) {
        requireKind(kind);
        return jdbc.query("select content_type, data from cabinet_image where cabinet_id = ? and kind = ?",
                (rs, row) -> new Image(rs.getString(1), rs.getBytes(2)), cabinetId, kind).stream().findFirst();
    }

    public BrandingDto.Response toResponse(Cabinet cabinet) {
        Long cabinetId = cabinet.getId();
        var rows = jdbc.query("select kind, extract(epoch from updated_at)::bigint from cabinet_image where cabinet_id = ?",
                (rs, row) -> new Object[] { rs.getString(1), rs.getLong(2) }, cabinetId);
        boolean logo = rows.stream().anyMatch(row -> "logo".equals(row[0]));
        boolean cover = rows.stream().anyMatch(row -> "cover".equals(row[0]));
        long version = rows.stream().mapToLong(row -> (Long) row[1]).max().orElse(0);
        return new BrandingDto.Response(cabinetId, cabinet.getName(), cabinet.getOwnerName(), cabinet.getTagline(),
                cabinet.getPrimaryColor(), cabinet.getAddress(), cabinet.getPhoneNumber(), cabinet.getEmail(),
                logo, cover, version);
    }

    /** Only real raster images: the type comes from the file content, never from the client. SVG is refused. */
    private String sniff(byte[] data) {
        if (data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length > 8 && Arrays.equals(Arrays.copyOf(data, 8),
                new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A })) {
            return "image/png";
        }
        if (data.length > 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        throw new IllegalArgumentException("Only JPG, PNG or WebP images are accepted.");
    }

    private void requireKind(String kind) {
        if (!"logo".equals(kind) && !"cover".equals(kind)) {
            throw new IllegalArgumentException("Image kind must be logo or cover.");
        }
    }

    private Cabinet find(Long id) {
        return cabinetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cabinet", id));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
