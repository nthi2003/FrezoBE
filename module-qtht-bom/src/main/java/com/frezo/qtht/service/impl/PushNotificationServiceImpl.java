package com.frezo.qtht.service.impl;

import com.frezo.qtht.entity.UserDevice;
import com.frezo.qtht.repository.UserDeviceRepository;
import com.frezo.qtht.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Expo Push implementation — POST batches tới https://exp.host/--/api/v2/push/send.
 * Batch tối đa 100 messages/request theo tài liệu Expo.
 * Auto-mark inactive khi Expo response chứa "DeviceNotRegistered".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final int MAX_BATCH = 100;

    private final UserDeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    @Override
    public void registerDevice(String username, String expoPushToken,
                               String platform, String deviceName,
                               String deviceId, String appVersion) {
        if (username == null || expoPushToken == null || expoPushToken.isBlank()) {
            log.warn("[Push] Skip register — missing username or token");
            return;
        }
        UserDevice device = deviceRepository.findByExpoPushToken(expoPushToken).orElseGet(UserDevice::new);
        device.setUsername(username);
        device.setExpoPushToken(expoPushToken);
        device.setPlatform(platform);
        device.setDeviceName(deviceName);
        device.setDeviceId(deviceId);
        device.setAppVersion(appVersion);
        device.setLastActiveAt(LocalDateTime.now());
        device.setIsActive(Boolean.TRUE);
        deviceRepository.save(device);
        log.info("[Push] Registered device for user={} platform={}", username, platform);
    }

    @Override
    public void unregisterDevice(String username, String expoPushToken) {
        deviceRepository.findByExpoPushToken(expoPushToken).ifPresent(d -> {
            if (username == null || username.equals(d.getUsername())) {
                d.setIsActive(Boolean.FALSE);
                deviceRepository.save(d);
            }
        });
    }

    @Async
    @Override
    public void sendToUser(String username, String title, String body, Map<String, Object> data) {
        sendToUsers(List.of(username), title, body, data);
    }

    @Async
    @Override
    public void sendToUsers(List<String> usernames, String title, String body, Map<String, Object> data) {
        if (usernames == null || usernames.isEmpty()) return;
        List<String> dedup = usernames.stream().filter(Objects::nonNull).distinct().toList();
        List<UserDevice> devices = deviceRepository.findByUsernameInAndIsActiveTrue(dedup);
        if (devices.isEmpty()) {
            log.debug("[Push] No active devices for users={}", dedup);
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (UserDevice d : devices) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("to", d.getExpoPushToken());
            msg.put("title", title);
            msg.put("body", body);
            msg.put("sound", "default");
            msg.put("priority", "high");
            if (data != null && !data.isEmpty()) msg.put("data", data);
            messages.add(msg);
        }

        // Batch 100/request theo doc Expo
        for (int i = 0; i < messages.size(); i += MAX_BATCH) {
            List<Map<String, Object>> batch = messages.subList(i, Math.min(i + MAX_BATCH, messages.size()));
            sendBatch(batch);
        }
    }

    private void sendBatch(List<Map<String, Object>> batch) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(batch, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(EXPO_PUSH_URL, entity, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("[Push] Expo non-2xx status={} body={}", resp.getStatusCode(), resp.getBody());
                return;
            }

            Object data = resp.getBody() != null ? resp.getBody().get("data") : null;
            if (data instanceof List<?> tickets) {
                for (int i = 0; i < tickets.size(); i++) {
                    Object t = tickets.get(i);
                    if (!(t instanceof Map<?, ?> ticket)) continue;
                    Object status = ticket.get("status");
                    if (!"ok".equals(status)) {
                        Object err = ticket.get("details") instanceof Map<?, ?> det ? det.get("error") : null;
                        Object token = batch.get(i).get("to");
                        log.warn("[Push] Ticket fail token={} status={} err={}", token, status, err);
                        if ("DeviceNotRegistered".equals(err) && token instanceof String s) {
                            deviceRepository.findByExpoPushToken(s).ifPresent(d -> {
                                d.setIsActive(Boolean.FALSE);
                                deviceRepository.save(d);
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Push] Batch send failed: {}", e.getMessage(), e);
        }
    }
}
