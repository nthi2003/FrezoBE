package com.frezo.common.helper;

import org.springframework.security.core.userdetails.UserDetails;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemUtils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    public static boolean isNotNullOrEmpty(String input) {
        return input != null && !input.isEmpty();
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static String escapeAndRemoveAccentsSqlLike(String keyword) {
        if (keyword == null)
            return null;
        return removeAccents(keyword).toLowerCase()
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%");
    }

    public static String removeAccents(String keyword) {
        if (keyword == null)
            return null;
        String normalized = Normalizer.normalize(keyword, Normalizer.Form.NFD);

        String noAccents = normalized.replaceAll("\\p{M}", "");

        noAccents = noAccents.replace("đ", "d").replace("Đ", "D");
        return noAccents;

    }

    public static String getCurrentUsername() {
        try {
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
            if (principal instanceof String) {
                return principal.toString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader("X-Forwarded-For");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }
}
