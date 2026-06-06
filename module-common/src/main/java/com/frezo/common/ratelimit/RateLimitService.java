package com.frezo.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    //  THINVQ Cache: key -> Bucket
    private final Cache<String, Bucket> ipCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).maximumSize(50_000).build();

    private final Cache<String, Bucket> userCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).maximumSize(10_000).build();

    private final Cache<String, Bucket> loginFailCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES).maximumSize(10_000).build();

    // THINVQ--- IP: 100 req/phút ---
    public boolean tryConsumeByIp(String ip) {
        Bucket bucket = ipCache.get(ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                        .build()
        );
        return bucket.tryConsume(1);
    }

    // THINVQ User plan: free=30 req/min, admin=200 req/min ---
    public boolean tryConsumeByUser(String userId, boolean isAdmin) {
        int limit = isAdmin ? 200 : 100;
        Bucket bucket = userCache.get(userId, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(limit, Refill.greedy(limit, Duration.ofMinutes(1))))
                        .build()
        );
        return bucket.tryConsume(1);
    }

    // THINVQ --- Brute force: 5 lần fail login / 15 phút ---
    public boolean tryLoginAttempt(String username) {
        Bucket bucket = loginFailCache.get(username, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15))))
                        .build()
        );
        return bucket.tryConsume(1);
    }

    public void resetLoginAttempts(String username) {
        loginFailCache.invalidate(username);
    }
}