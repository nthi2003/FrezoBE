package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.request.LivestreamEventRequest;
import com.frezo.fbautomation.dto.response.LivestreamEventResponse;
import com.frezo.fbautomation.entity.LivestreamEvent;
import com.frezo.fbautomation.repository.LivestreamEventRepository;
import com.frezo.fbautomation.service.LivestreamEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LivestreamEventServiceImpl implements LivestreamEventService {

    private final LivestreamEventRepository repository;

    @Override
    public List<LivestreamEventResponse> list(String status) {
        return repository.findAll().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .filter(e -> status == null || status.isBlank() || status.equalsIgnoreCase(e.getStatus()))
                .sorted(Comparator.comparing(LivestreamEvent::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public LivestreamEventResponse get(String id) {
        return toResponse(mustFind(id));
    }

    @Override
    @Transactional
    public LivestreamEventResponse create(LivestreamEventRequest req) {
        LivestreamEvent e = LivestreamEvent.builder()
                .title(req.getTitle().trim())
                .channel(nz(req.getChannel(), "FACEBOOK"))
                .scheduledAt(req.getScheduledAt())
                .durationMinutes(req.getDurationMinutes() != null ? req.getDurationMinutes() : 60)
                .notifyBeforeMinutes(req.getNotifyBeforeMinutes() != null ? req.getNotifyBeforeMinutes() : 30)
                .status(nz(req.getStatus(), "SCHEDULED"))
                .registrantCount(req.getRegistrantCount() != null ? req.getRegistrantCount() : 0)
                .streamUrl(req.getStreamUrl())
                .note(req.getNote())
                .build();
        return toResponse(repository.save(e));
    }

    @Override
    @Transactional
    public LivestreamEventResponse update(String id, LivestreamEventRequest req) {
        LivestreamEvent e = mustFind(id);
        e.setTitle(req.getTitle().trim());
        if (req.getChannel() != null) e.setChannel(req.getChannel());
        e.setScheduledAt(req.getScheduledAt());
        if (req.getDurationMinutes() != null) e.setDurationMinutes(req.getDurationMinutes());
        if (req.getNotifyBeforeMinutes() != null) e.setNotifyBeforeMinutes(req.getNotifyBeforeMinutes());
        if (req.getStatus() != null) e.setStatus(req.getStatus());
        if (req.getRegistrantCount() != null) e.setRegistrantCount(req.getRegistrantCount());
        e.setStreamUrl(req.getStreamUrl());
        e.setNote(req.getNote());
        return toResponse(repository.save(e));
    }

    @Override
    @Transactional
    public void delete(String id) {
        LivestreamEvent e = mustFind(id);
        e.setIsDeleted(true);
        repository.save(e);
    }

    @Override
    @Transactional
    public LivestreamEventResponse markNotified(String id) {
        LivestreamEvent e = mustFind(id);
        e.setNotifiedAt(OffsetDateTime.now());
        return toResponse(repository.save(e));
    }

    @Override
    @Transactional
    public LivestreamEventResponse updateStatus(String id, String status) {
        LivestreamEvent e = mustFind(id);
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("SCHEDULED", "LIVE", "ENDED", "CANCELLED").contains(s)) {
            throw new IllegalArgumentException("status phải là SCHEDULED|LIVE|ENDED|CANCELLED");
        }
        e.setStatus(s);
        return toResponse(repository.save(e));
    }

    @Override
    public Map<String, Object> dashboard() {
        List<LivestreamEvent> all = repository.findAll().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted())).toList();
        OffsetDateTime now = OffsetDateTime.now();
        List<LivestreamEventResponse> upcoming = all.stream()
                .filter(e -> "SCHEDULED".equals(e.getStatus()))
                .filter(e -> e.getScheduledAt() != null && e.getScheduledAt().isAfter(now))
                .sorted(Comparator.comparing(LivestreamEvent::getScheduledAt))
                .limit(5)
                .map(this::toResponse)
                .toList();
        Map<String, Object> m = new HashMap<>();
        m.put("total", all.size());
        m.put("scheduled", all.stream().filter(e -> "SCHEDULED".equals(e.getStatus())).count());
        m.put("live", all.stream().filter(e -> "LIVE".equals(e.getStatus())).count());
        m.put("ended", all.stream().filter(e -> "ENDED".equals(e.getStatus())).count());
        m.put("needsNotify", all.stream().map(this::toResponse).filter(r -> Boolean.TRUE.equals(r.getNeedsNotify())).count());
        m.put("upcoming", upcoming);
        return m;
    }

    private LivestreamEvent mustFind(String id) {
        LivestreamEvent e = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy livestream"));
        if (Boolean.TRUE.equals(e.getIsDeleted())) throw new IllegalArgumentException("Livestream đã xoá");
        return e;
    }

    private LivestreamEventResponse toResponse(LivestreamEvent e) {
        LivestreamEventResponse out = new LivestreamEventResponse();
        out.setId(e.getId());
        out.setTitle(e.getTitle());
        out.setChannel(e.getChannel());
        out.setScheduledAt(e.getScheduledAt());
        out.setDurationMinutes(e.getDurationMinutes());
        out.setNotifyBeforeMinutes(e.getNotifyBeforeMinutes());
        out.setStatus(e.getStatus());
        out.setRegistrantCount(e.getRegistrantCount());
        out.setNotifiedAt(e.getNotifiedAt());
        out.setStreamUrl(e.getStreamUrl());
        out.setNote(e.getNote());
        out.setCreatedDate(e.getCreatedDate());
        out.setNeedsNotify(computeNeedsNotify(e));
        return out;
    }

    private boolean computeNeedsNotify(LivestreamEvent e) {
        if (!"SCHEDULED".equals(e.getStatus()) || e.getScheduledAt() == null || e.getNotifiedAt() != null) {
            return false;
        }
        int before = e.getNotifyBeforeMinutes() == null ? 30 : e.getNotifyBeforeMinutes();
        OffsetDateTime window = e.getScheduledAt().minusMinutes(before);
        OffsetDateTime now = OffsetDateTime.now();
        return !now.isBefore(window) && now.isBefore(e.getScheduledAt());
    }

    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }
}
