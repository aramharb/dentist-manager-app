package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.entity.ClientAccount;
import com.example.demo.entity.LoginUser;
import com.example.demo.repository.ClientAccountRepository;
import com.example.demo.repository.LoginUserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtTokenService {
    private static final String CLIENT_KIND = "client";
    private static final String HEADER = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final LoginUserRepository userRepository;
    private final ClientAccountRepository clientRepository;
    private final byte[] secret;
    private final long lifetimeSeconds;

    public JwtTokenService(ObjectMapper objectMapper, LoginUserRepository userRepository,
            @Value("${app.auth.jwt-secret:change-this-development-jwt-secret-before-production-2026}") String secret,
            @Value("${app.auth.jwt-lifetime-seconds:28800}") long lifetimeSeconds) {
        this(objectMapper, userRepository, null, secret, lifetimeSeconds);
    }

    @Autowired
    public JwtTokenService(ObjectMapper objectMapper, LoginUserRepository userRepository,
            ClientAccountRepository clientRepository,
            @Value("${app.auth.jwt-secret:change-this-development-jwt-secret-before-production-2026}") String secret,
            @Value("${app.auth.jwt-lifetime-seconds:28800}") long lifetimeSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.auth.jwt-secret must contain at least 32 bytes.");
        }
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public String issue(LoginUser user) {
        return sign(user.getUsername(), user.getId(), user.getRole(), null);
    }

    /** Token of a client (patient) account; it only opens the /api/client endpoints. */
    public String issueClient(ClientAccount account) {
        return sign(account.getPhone(), account.getId(), ClinicPrincipal.CLIENT_ROLE, CLIENT_KIND);
    }

    private String sign(String subject, Long id, String role, String kind) {
        long issuedAt = Instant.now().getEpochSecond();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", subject);
        claims.put("uid", id);
        claims.put("role", role);
        if (kind != null) claims.put("kind", kind);
        claims.put("iat", issuedAt);
        claims.put("exp", issuedAt + lifetimeSeconds);
        try {
            String payload = base64Url(objectMapper.writeValueAsBytes(claims));
            String unsignedToken = HEADER + "." + payload;
            return unsignedToken + "." + base64Url(sign(unsignedToken));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to issue authentication token.", exception);
        }
    }

    public ClinicPrincipal verify(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                throw invalidToken();
            }
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!parts[2].equals(base64Url(actualSignature))
                    || !MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw invalidToken();
            }
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long userId = claims.path("uid").asLong(-1);
            long expiresAt = claims.path("exp").asLong(0);
            String username = claims.path("sub").asText("");
            if (userId <= 0 || username.isBlank() || expiresAt <= Instant.now().getEpochSecond()) {
                throw invalidToken();
            }
            if (CLIENT_KIND.equals(claims.path("kind").asText(""))) {
                if (clientRepository == null) throw invalidToken();
                ClientAccount account = clientRepository.findById(userId)
                        .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                        .filter(candidate -> candidate.getPhone().equals(username))
                        .orElseThrow(this::invalidToken);
                return new ClinicPrincipal(account.getId(), account.getPhone(), ClinicPrincipal.CLIENT_ROLE, null);
            }
            LoginUser user = userRepository.findById(userId)
                    .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                    .filter(candidate -> candidate.getUsername().equalsIgnoreCase(username))
                    .orElseThrow(this::invalidToken);
            if (user.getCabinetId() != null && !userRepository.isCabinetActive(user.getCabinetId())) {
                throw invalidToken();
            }
            return new ClinicPrincipal(user.getId(), user.getUsername(), user.getRole(), user.getCabinetId());
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid or expired authentication token.");
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
