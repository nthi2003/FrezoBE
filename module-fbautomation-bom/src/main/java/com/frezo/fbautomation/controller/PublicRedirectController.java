package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.service.AffiliateLinkService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * PublicRedirectController — GET /public/r/{code}
 * <p>
 * Xử lý click affiliate link:
 * <ol>
 *   <li>Resolve slug → targetUrl (đã gắn UTM)</li>
 *   <li>Ghi {@code AffiliateClick} + tăng counter</li>
 *   <li>Response 302 sang targetUrl</li>
 * </ol>
 * <p>
 * Được whitelist qua rule GET {@code /public/**} trong SecurityConfig, không cần JWT.
 */
@Slf4j
@RestController
@RequestMapping("/public/r")
@RequiredArgsConstructor
public class PublicRedirectController {

    private final AffiliateLinkService affiliateService;

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest req) {
        try {
            String target = affiliateService.trackAndResolve(
                    code,
                    clientIp(req),
                    req.getHeader("User-Agent"),
                    req.getHeader("Referer")
            );
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
        } catch (IllegalStateException gone) {
            // EXPIRED/PAUSED — trả 410 Gone (chuẩn hơn 404) để crawler biết không quay lại.
            log.info("Affiliate '{}' gone: {}", code, gone.getMessage());
            return ResponseEntity.status(HttpStatus.GONE).build();
        } catch (IllegalArgumentException notFound) {
            log.info("Affiliate '{}' not found", code);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Ưu tiên X-Forwarded-For (chuỗi IP, lấy IP đầu = client thật). Fallback RemoteAddr.
     * Copy nhỏ cùng logic với {@code PublicLeadController} để không tạo dependency chéo.
     */
    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }
}
