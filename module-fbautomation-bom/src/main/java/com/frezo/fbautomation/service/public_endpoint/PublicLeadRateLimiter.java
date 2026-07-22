package com.frezo.fbautomation.service.public_endpoint;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiter đơn giản in-memory theo IP cho public endpoints (landing form, Zalo webhook).
 * <ul>
 *   <li>Sliding-window: cho phép <b>N request / window</b>.</li>
 *   <li>Không dùng Redis vì workload thấp + không cần chia sẻ giữa nhiều instance.
 *       Nếu scale-out, thay bằng bucket4j-redis.</li>
 * </ul>
 */
@Component
public class PublicLeadRateLimiter {

    private static final int MAX_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    // Cleanup thủ công định kỳ để tránh mem leak — key IP đã idle > 1h sẽ bị drop khi truy cập kế tiếp.
    private static final Duration IDLE_TTL = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> ipHits = new ConcurrentHashMap<>();

    /**
     * @return true nếu request được phép; false nếu đã vượt hạn ngạch.
     */
    public boolean allow(String ip) {
        if (ip == null || ip.isBlank()) return true; // không xác định IP → không chặn
        Instant now = Instant.now();
        Deque<Instant> hits = ipHits.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (hits) {
            // Drop hits ngoài window
            while (!hits.isEmpty() && Duration.between(hits.peekFirst(), now).compareTo(WINDOW) > 0) {
                hits.pollFirst();
            }
            if (hits.size() >= MAX_PER_WINDOW) {
                return false;
            }
            hits.addLast(now);
        }
        cleanupIdle(now);
        return true;
    }

    /**
     * Cleanup nhẹ — chỉ chạy khi có request, tránh cần Scheduler.
     */
    private void cleanupIdle(Instant now) {
        // Chỉ scan 1% xác suất → tránh overhead khi traffic cao
        if (Math.random() > 0.01) return;
        ipHits.entrySet().removeIf(e -> {
            Deque<Instant> q = e.getValue();
            Instant last;
            synchronized (q) {
                last = q.peekLast();
            }
            return last == null || Duration.between(last, now).compareTo(IDLE_TTL) > 0;
        });
    }
}
