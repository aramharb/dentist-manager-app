package com.example.demo.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.service.RecordAccessLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes an access-log entry for every successful read or change of a client file or clinical record. */
@Component
public class RecordAccessInterceptor implements HandlerInterceptor {
    private static final Pattern RECORD = Pattern.compile(
            "^/api/(patients|treatments|procedures|photos|prescriptions)/(\\d+)(?:/([a-z-]+))?/?$");
    private static final Pattern PATIENT_CREATE = Pattern.compile("^/api/patients/?$");

    private final RecordAccessLogService logService;

    public RecordAccessInterceptor(RecordAccessLogService logService) {
        this.logService = logService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception) {
        if (exception != null || response.getStatus() >= 400 || !(request.getUserPrincipal() instanceof ClinicPrincipal actor)) {
            return;
        }
        String action = switch (request.getMethod()) {
            case "GET" -> "VIEW";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> null;
        };
        if (action == null) return;
        String path = request.getRequestURI();
        if (PATIENT_CREATE.matcher(path).matches()) {
            if ("CREATE".equals(action)) logService.record(actor, action, "patient", null, request.getRemoteAddr());
            return;
        }
        Matcher match = RECORD.matcher(path);
        if (!match.matches()) return;
        String resource = singular(match.group(1));
        String part = match.group(3);
        if (part != null) resource = resource + "/" + part;
        logService.record(actor, action, resource, Long.valueOf(match.group(2)), request.getRemoteAddr());
    }

    private static String singular(String plural) {
        return switch (plural) {
            case "patients" -> "patient";
            case "treatments" -> "treatment";
            case "procedures" -> "procedure";
            case "photos" -> "photo";
            default -> "prescription";
        };
    }
}
