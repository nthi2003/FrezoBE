package com.frezo.fbautomation.controller;

import com.frezo.common.service.NotificationService;
import com.frezo.fbautomation.dto.request.PublicLeadRequest;
import com.frezo.fbautomation.entity.FacebookLead;
import com.frezo.fbautomation.repository.FacebookLeadRepository;
import com.frezo.fbautomation.service.public_endpoint.PublicLeadRateLimiter;
import com.frezo.fbautomation.service.public_endpoint.ZaloWebhookVerifier;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public endpoints — KHÔNG cần JWT.
 * <p>
 * Được whitelist trong SecurityConfig ({@code /api/public/**}).
 * <ul>
 *   <li>POST /api/public/leads — landing page contact form.</li>
 *   <li>POST /api/public/zalo/webhook — Zalo OA callback (verify signature).</li>
 * </ul>
 * <p>
 * Anti-abuse: honeypot + timestamp check + IP rate limit.
 */
/**
 * Note về prefix: BE có context-path {@code /api} (application.yml).
 * Controller mapping {@code /public/inbox} → full URL client gọi là
 * {@code https://<host>/api/public/inbox/...}.
 * Whitelist tương ứng ở SecurityConfig là {@code /public/**} (đã có sẵn),
 * cụ thể là {@code POST /public/inbox/leads} và {@code /public/inbox/zalo/webhook}
 * được permitAll trong config.
 */
@Slf4j
@RestController
@RequestMapping("/public/inbox")
@RequiredArgsConstructor
@Tag(name = "Public Leads", description = "Public endpoints cho landing page & Zalo webhook")
public class PublicLeadController {

    private final FacebookLeadRepository leadRepository;
    private final PublicLeadRateLimiter rateLimiter;
    private final ZaloWebhookVerifier zaloVerifier;
    private final NotificationService notificationService;

    /**
     * Danh sách username được nhận thông báo bell khi có lead mới.
     * Cấu hình qua {@code frezo.inbox.notify-users} (comma-separated).
     * <p>
     * VD: {@code frezo.inbox.notify-users=admin,cskh1,cskh2}
     */
    @Value("${frezo.inbox.notify-users:admin}")
    private String notifyUsersRaw;

    private List<String> getNotifyUsers() {
        if (notifyUsersRaw == null || notifyUsersRaw.isBlank()) return Collections.emptyList();
        return Arrays.stream(notifyUsersRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // ============================================================
    // 1. LANDING PAGE — contact form
    // ============================================================

    /**
     * URL đầy đủ client gọi: {@code POST /api/public/inbox/leads} (context-path prepend).
     */
    @Operation(summary = "Landing page contact form submit")
    @PostMapping("/leads")
    public ResponseEntity<Response<Map<String, Object>>> submitLandingLead(
            @Valid @RequestBody PublicLeadRequest req,
            HttpServletRequest http) {

        String ip = getClientIp(http);
        String referer = http.getHeader("Referer");

        // ---- Anti-spam checks (fail silently: trả 200 để bot không biết) ----
        if (req.get_hp() != null && !req.get_hp().isBlank()) {
            log.warn("[public-lead] Honeypot triggered ip={} name={}", ip, req.getName());
            return ResponseEntity.ok(Response.ok(Map.of("ok", true))); // giả success
        }
        if (req.get_ts() != null && (System.currentTimeMillis() - req.get_ts()) < 1500) {
            log.warn("[public-lead] Submitted too fast ip={} delta={}ms", ip, System.currentTimeMillis() - req.get_ts());
            return ResponseEntity.ok(Response.ok(Map.of("ok", true))); // giả success
        }
        if ((req.getPhone() == null || req.getPhone().isBlank())
                && (req.getEmail() == null || req.getEmail().isBlank())) {
            return ResponseEntity.badRequest().body(
                    Response.error("Vui lòng cung cấp SĐT hoặc email để chúng tôi liên hệ"));
        }
        if (!rateLimiter.allow(ip)) {
            log.warn("[public-lead] Rate limit hit ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    Response.error("Bạn gửi quá nhanh, vui lòng thử lại sau vài phút."));
        }

        FacebookLead lead = FacebookLead.builder()
                .name(safe(req.getName()))
                .phone(safe(req.getPhone()))
                .email(safe(req.getEmail()))
                .subject(safe(req.getSubject()))
                .message(safe(req.getMessage()))
                .source("LANDING")
                .status("NEW")
                .sourceIp(ip)
                .referer(safe(referer))
                .note("[Landing page] " + (req.getSubject() != null ? req.getSubject() : ""))
                .build();

        FacebookLead saved = leadRepository.save(lead);
        log.info("[public-lead] New landing lead id={} name={} ip={}", saved.getId(), saved.getName(), ip);

        // ---- Notify all CSKH agents ----
        // 2 kênh song song:
        //   1) DB Notification (persist) — bell icon polling sẽ thấy.
        //   2) WebSocket /topic/leads-new — nếu ai đang mở tab inbox thì cập nhật realtime.
        notifyCskhStaff(saved, "LANDING");

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("id", saved.getId());
        result.put("message", "Cảm ơn bạn đã liên hệ. Chúng tôi sẽ phản hồi sớm nhất.");
        return ResponseEntity.ok(Response.ok(result));
    }

    /**
     * Ghim thông báo cho toàn bộ nhân viên CSKH khi có lead mới.
     *
     * @param lead   entity đã lưu
     * @param source "LANDING" hoặc "ZALO"
     */
    private void notifyCskhStaff(FacebookLead lead, String source) {
        List<String> recipients = getNotifyUsers();
        if (recipients.isEmpty()) {
            log.warn("[public-lead] frezo.inbox.notify-users trống — không ai được ghim thông báo");
            return;
        }

        String sourceLabel = "LANDING".equals(source) ? "Landing page" : "Zalo OA";
        String title = "Khách hàng mới từ " + sourceLabel;
        String preview = lead.getMessage() != null && !lead.getMessage().isBlank()
                ? lead.getMessage()
                : (lead.getSubject() != null ? lead.getSubject() : "(chưa có nội dung)");
        if (preview.length() > 140) preview = preview.substring(0, 140) + "...";
        String message = (lead.getName() != null ? lead.getName() : "Khách chưa xác định")
                + " · " + preview;

        // Deep-link: /mkt/inbox?highlight={id} → FE tự mở drawer chi tiết lead.
        String actionUrl = "/mkt/inbox?highlight=" + lead.getId();

        try {
            notificationService.notifyMany(
                    recipients,
                    title,
                    message,
                    "LEAD_NEW",           // FE map sang icon
                    "LEAD",               // entityType
                    lead.getId(),         // entityId
                    actionUrl,
                    null,                 // senderUsername (system event)
                    false                 // không urgent — không cần email
            );
        } catch (Exception e) {
            log.warn("[public-lead] Không ghi được DB notification: {}", e.getMessage());
        }

        // WebSocket realtime — nếu inbox đang mở thì auto-append row.
        try {
            notificationService.sendToTopic("/topic/leads-new", Map.of(
                    "id", lead.getId(),
                    "name", lead.getName() != null ? lead.getName() : "",
                    "source", source,
                    "message", lead.getMessage() != null ? lead.getMessage() : "",
                    "createdDate", lead.getCreatedDate() != null ? lead.getCreatedDate().toString() : ""
            ));
        } catch (Exception e) {
            log.warn("[public-lead] WebSocket push fail: {}", e.getMessage());
        }
    }

    // ============================================================
    // 2. ZALO OA WEBHOOK
    // ============================================================

    @Operation(summary = "Zalo OA webhook — Verify GET (Zalo config check)")
    @GetMapping("/zalo/webhook")
    public ResponseEntity<String> verifyZaloWebhook(@RequestParam(required = false) String challenge) {
        // Zalo OA đôi khi ping verify — echo challenge nếu có.
        return ResponseEntity.ok(challenge != null ? challenge : "OK");
    }

    @Operation(summary = "Zalo OA webhook — nhận tin nhắn từ khách")
    @PostMapping("/zalo/webhook")
    public ResponseEntity<Response<Map<String, Object>>> onZaloEvent(
            @RequestBody Map<String, Object> event,
            @RequestHeader(value = "X-ZEvent-Signature", required = false) String signature,
            HttpServletRequest http) {

        String ip = getClientIp(http);
        log.info("[zalo-webhook] Event received type={} ip={}", event.get("event_name"), ip);

        // Verify signature (nếu app secret đã config)
        if (!zaloVerifier.verify(event, signature)) {
            log.warn("[zalo-webhook] Invalid signature — reject event");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Response.error("Invalid signature"));
        }

        String eventName = String.valueOf(event.getOrDefault("event_name", ""));
        // Zalo gửi nhiều loại event; chỉ handle "user_send_text" / "user_send_link" thành lead.
        if (!eventName.startsWith("user_send_")) {
            return ResponseEntity.ok(Response.ok(Map.of("ok", true, "ignored", eventName)));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> sender = event.get("sender") instanceof Map<?, ?> s
                ? (Map<String, Object>) s : java.util.Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> message = event.get("message") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : java.util.Collections.emptyMap();

        String zaloUserId = String.valueOf(sender.getOrDefault("id", ""));
        String text = String.valueOf(message.getOrDefault("text", ""));
        String oaId = String.valueOf(event.getOrDefault("oa_id", ""));

        FacebookLead lead = FacebookLead.builder()
                .name("Khách Zalo " + (zaloUserId.length() >= 6 ? zaloUserId.substring(0, 6) : zaloUserId))
                .message(text)
                .subject("Tin nhắn từ Zalo OA")
                .source("ZALO")
                .status("NEW")
                .sourceIp(ip)
                .profileUrl("https://chat.zalo.me/#/personal/" + zaloUserId)
                .referer("zalo-oa:" + oaId)
                .note("[Zalo] userId=" + zaloUserId + " oaId=" + oaId)
                .build();

        FacebookLead saved = leadRepository.save(lead);
        notifyCskhStaff(saved, "ZALO");

        return ResponseEntity.ok(Response.ok(Map.of("ok", true, "id", saved.getId())));
    }

    // ---- Utils ----
    private static String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Nginx / LB có thể set nhiều IP → lấy IP đầu (client thật).
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }

    /** Trim + null-safe để tránh lưu whitespace trần vào DB. */
    private static String safe(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
