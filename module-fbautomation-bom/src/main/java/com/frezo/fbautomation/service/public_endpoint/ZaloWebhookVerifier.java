package com.frezo.fbautomation.service.public_endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verify chữ ký webhook Zalo OA theo tài liệu chính thức:
 * <a href="https://developers.zalo.me/docs/api/official-account-api/su-kien/su-kien-nguoi-dung-nhan-tin-cho-oa-post-4712">
 * event verification</a>.
 * <p>
 * Signature format: {@code mac = SHA256(app_id + timestamp + rawBody + oa_secret_key)}.
 * Nếu {@code zalo.oa.secret} chưa config → skip verify (dev mode) và log warning.
 * Ở production BẮT BUỘC set secret trong Consul / env.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZaloWebhookVerifier {

    @Value("${zalo.oa.app-id:}")
    private String appId;

    @Value("${zalo.oa.secret:}")
    private String appSecret;

    @Value("${zalo.oa.verify-enabled:false}")
    private boolean verifyEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param event Payload đã parse (do Spring deserialize).
     * @param signature Header {@code X-ZEvent-Signature} do Zalo gửi.
     * @return true nếu hợp lệ hoặc verify tắt.
     */
    public boolean verify(Map<String, Object> event, String signature) {
        if (!verifyEnabled) {
            if (appSecret == null || appSecret.isBlank()) {
                log.warn("[zalo] verify disabled — set zalo.oa.secret + zalo.oa.verify-enabled=true để bật ở prod.");
            }
            return true;
        }
        if (appSecret == null || appSecret.isBlank()) {
            log.error("[zalo] verify-enabled=true nhưng secret trống — reject tất cả event.");
            return false;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[zalo] Header X-ZEvent-Signature vắng — reject.");
            return false;
        }
        try {
            // Zalo yêu cầu re-serialize theo key alphabet order để đảm bảo consistent bytes.
            String canonical = objectMapper.writeValueAsString(new TreeMap<>(event));
            Object tsObj = event.get("timestamp");
            String timestamp = tsObj == null ? "" : String.valueOf(tsObj);

            String raw = appId + timestamp + canonical + appSecret;
            String computed = sha256Hex(raw);
            boolean ok = MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    signature.replace("mac=", "").getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                log.warn("[zalo] Signature mismatch. expected={} got={}", computed, signature);
            }
            return ok;
        } catch (Exception e) {
            log.error("[zalo] Verify error", e);
            return false;
        }
    }

    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
