package com.frezo.event.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.FePage;
import com.frezo.common.service.NotificationService;
import com.frezo.event.dto.request.EventSaveRequest;
import com.frezo.event.dto.request.RsvpRequest;
import com.frezo.event.dto.response.EventDto;
import com.frezo.event.dto.response.EventRegistrationDto;
import com.frezo.event.entity.Event;
import com.frezo.event.entity.EventRegistration;
import com.frezo.event.repository.EventRegistrationRepository;
import com.frezo.event.repository.EventRepository;
import com.frezo.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String RSVP_GOING = "GOING";
    public static final String RSVP_MAYBE = "MAYBE";
    public static final String RSVP_DECLINED = "DECLINED";
    public static final String RSVP_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final NotificationService notificationService;

    /**
     * Liệt kê tất cả sự kiện cho trang quản trị, có thể lọc theo trạng thái.
     */
    @Override
    public FePage<EventDto> listAdmin(String status) {
        List<Event> all = eventRepository.findByIsDeletedFalseOrderByStartAtDesc();
        if (status != null && !status.isBlank()) {
            all = all.stream().filter(e -> status.equalsIgnoreCase(e.getStatus())).toList();
        }
        return FePage.all(all.stream().map(e -> toDto(e, null)).toList());
    }

    /**
     * Lấy thông tin chi tiết một sự kiện theo mã định danh.
     */
    @Override
    public EventDto get(String id) {
        return toDto(find(id), null);
    }

    /**
     * Tạo mới sự kiện ở trạng thái nháp (DRAFT).
     */
    @Override
    @Transactional
    public EventDto create(EventSaveRequest req) {
        validateSave(req);
        Event e = Event.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .location(req.getLocation())
                .startAt(parseDt(req.getStartAt()))
                .endAt(req.getEndAt() != null ? parseDt(req.getEndAt()) : null)
                .status(STATUS_DRAFT)
                .capacity(req.getCapacity())
                .registeredCount(0)
                .coverUrl(req.getCoverUrl())
                .organizerUsername(SystemUtils.getCurrentUsername())
                .build();
        e.setId(UUID.randomUUID().toString());
        return toDto(eventRepository.save(e), null);
    }

    /**
     * Cập nhật thông tin sự kiện. Không cho phép sửa sự kiện đã bị huỷ.
     */
    @Override
    @Transactional
    public EventDto update(String id, EventSaveRequest req) {
        Event e = find(id);
        if (STATUS_CANCELLED.equals(e.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT);
        }
        validateSave(req);
        e.setTitle(req.getTitle().trim());
        e.setDescription(req.getDescription());
        e.setLocation(req.getLocation());
        e.setStartAt(parseDt(req.getStartAt()));
        e.setEndAt(req.getEndAt() != null ? parseDt(req.getEndAt()) : null);
        e.setCapacity(req.getCapacity());
        e.setCoverUrl(req.getCoverUrl());
        return toDto(eventRepository.save(e), null);
    }

    /**
     * Xóa mềm sự kiện (đánh dấu isDeleted = true).
     */
    @Override
    @Transactional
    public void delete(String id) {
        Event e = find(id);
        e.setIsDeleted(true);
        eventRepository.save(e);
    }

    /**
     * Công bố sự kiện (PUBLISHED) và gửi thông báo cho người tổ chức.
     */
    @Override
    @Transactional
    public EventDto publish(String id) {
        Event e = find(id);
        if (STATUS_CANCELLED.equals(e.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT);
        }
        e.setStatus(STATUS_PUBLISHED);
        e.setPublishedAt(LocalDateTime.now());
        Event saved = eventRepository.save(e);
        notifyOrganizer(saved, "Sự kiện đã publish",
                saved.getTitle() + " đã được công bố — mở đăng ký.");
        return toDto(saved, null);
    }

    /**
     * Huỷ sự kiện và thông báo cho tất cả người tham gia đã đăng ký.
     */
    @Override
    @Transactional
    public EventDto cancel(String id) {
        Event e = find(id);
        e.setStatus(STATUS_CANCELLED);
        e.setCancelledAt(LocalDateTime.now());
        Event saved = eventRepository.save(e);
        // Notify registered GOING users
        registrationRepository.findByEventIdAndIsDeletedFalseOrderByRegisteredAtDesc(id).stream()
                .filter(r -> RSVP_GOING.equals(r.getRsvpStatus()) || RSVP_MAYBE.equals(r.getRsvpStatus()))
                .forEach(r -> notificationService.notify(
                        r.getUsername(),
                        "Sự kiện bị huỷ",
                        saved.getTitle() + " đã bị huỷ.",
                        "EVENT_CANCELLED",
                        "EVENT",
                        saved.getId(),
                        "/admin/events?id=" + saved.getId(),
                        SystemUtils.getCurrentUsername(),
                        false));
        return toDto(saved, null);
    }

    /**
     * Lấy danh sách sự kiện trong khoảng ngày cho chế độ xem lịch.
     */
    @Override
    public List<EventDto> calendar(String from, String to) {
        LocalDateTime f = from != null ? parseDt(from.contains("T") ? from : from + "T00:00:00")
                : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime t = to != null ? parseDt(to.contains("T") ? to : to + "T23:59:59")
                : f.plusMonths(2);
        return eventRepository.findByStartAtBetweenAndIsDeletedFalseOrderByStartAtAsc(f, t).stream()
                .map(e -> toDto(e, null))
                .toList();
    }

    /**
     * Liệt kê danh sách đăng ký tham gia của một sự kiện.
     */
    @Override
    public List<EventRegistrationDto> listRegistrations(String eventId) {
        find(eventId);
        return registrationRepository.findByEventIdAndIsDeletedFalseOrderByRegisteredAtDesc(eventId)
                .stream().map(r -> toRegDto(r, null)).toList();
    }

    /**
     * Lấy danh sách sự kiện đã công bố hiển thị cho người dùng trên portal.
     */
    @Override
    public FePage<EventDto> listPortal() {
        String me = SystemUtils.getCurrentUsername();
        List<EventDto> list = eventRepository
                .findByStatusAndIsDeletedFalseOrderByStartAtAsc(STATUS_PUBLISHED).stream()
                .filter(e -> e.getStartAt() == null || e.getStartAt().isAfter(LocalDateTime.now().minusDays(1)))
                .map(e -> {
                    String my = registrationRepository
                            .findByEventIdAndUsernameAndIsDeletedFalse(e.getId(), me)
                            .map(EventRegistration::getRsvpStatus).orElse(null);
                    return toDto(e, my);
                })
                .toList();
        return FePage.all(list);
    }

    /**
     * Lấy chi tiết sự kiện trên portal, chỉ cho phép xem sự kiện đã công bố.
     */
    @Override
    public EventDto getPortal(String id) {
        Event e = find(id);
        if (!STATUS_PUBLISHED.equals(e.getStatus())) {
            throw new AppException(CommonErrorCode.NOT_FOUND, "Sự kiện không khả dụng");
        }
        String me = SystemUtils.getCurrentUsername();
        String my = registrationRepository.findByEventIdAndUsernameAndIsDeletedFalse(id, me)
                .map(EventRegistration::getRsvpStatus).orElse(null);
        return toDto(e, my);
    }

    /**
     * Đăng ký / thay đổi trạng thái RSVP của người dùng cho một sự kiện.
     * Kiểm tra dung lượng trước khi xác nhận tham gia (GOING).
     */
    @Override
    @Transactional
    public EventRegistrationDto rsvp(String eventId, RsvpRequest req) {
        Event e = find(eventId);
        if (!STATUS_PUBLISHED.equals(e.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ RSVP khi event PUBLISHED");
        }
        String status = req != null && req.getStatus() != null
                ? req.getStatus().trim().toUpperCase() : RSVP_GOING;
        if (!RSVP_GOING.equals(status) && !RSVP_MAYBE.equals(status) && !RSVP_DECLINED.equals(status)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "status phải GOING|MAYBE|DECLINED");
        }

        String me = SystemUtils.getCurrentUsername();
        if (me == null || me.isBlank()) {
            throw new AppException(CommonErrorCode.UNAUTHORIZED, "Cần đăng nhập để RSVP");
        }

        EventRegistration reg = registrationRepository
                .findByEventIdAndUsernameAndIsDeletedFalse(eventId, me)
                .orElse(null);

        String prev = reg != null ? reg.getRsvpStatus() : null;
        boolean wasGoing = RSVP_GOING.equals(prev);
        boolean willGoing = RSVP_GOING.equals(status);

        if (willGoing && !wasGoing) {
            int going = (int) registrationRepository
                    .countByEventIdAndRsvpStatusAndIsDeletedFalse(eventId, RSVP_GOING);
            if (e.getCapacity() != null && going >= e.getCapacity()) {
                throw new AppException(CommonErrorCode.CONFLICT, "Sự kiện đã đủ chỗ");
            }
        }

        if (reg == null) {
            reg = EventRegistration.builder()
                    .eventId(eventId)
                    .username(me)
                    .registeredAt(LocalDateTime.now())
                    .build();
            reg.setId(UUID.randomUUID().toString());
        }
        reg.setRsvpStatus(status);
        reg.setNote(req != null ? req.getNote() : null);
        reg.setDisplayName(req != null && req.getDisplayName() != null ? req.getDisplayName() : me);
        reg.setEmail(req != null ? req.getEmail() : null);
        reg.setRegisteredAt(LocalDateTime.now());
        reg = registrationRepository.save(reg);

        refreshRegisteredCount(e);
        notificationService.notify(
                me,
                "RSVP: " + status,
                e.getTitle() + " — trạng thái " + status,
                "EVENT_RSVP",
                "EVENT",
                e.getId(),
                "/admin/events?id=" + e.getId(),
                "system",
                false);
        return toRegDto(reg, e.getTitle());
    }

    /**
     * Hủy đăng ký RSVP của người dùng hiện tại cho sự kiện.
     */
    @Override
    @Transactional
    public void cancelRsvp(String eventId) {
        String me = SystemUtils.getCurrentUsername();
        EventRegistration reg = registrationRepository
                .findByEventIdAndUsernameAndIsDeletedFalse(eventId, me)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Chưa đăng ký"));
        reg.setRsvpStatus(RSVP_CANCELLED);
        registrationRepository.save(reg);
        Event e = find(eventId);
        refreshRegisteredCount(e);
    }

    /**
     * Lấy danh sách các sự kiện mà người dùng hiện tại đã đăng ký tham gia.
     */
    @Override
    public List<EventRegistrationDto> myRegistrations() {
        String me = SystemUtils.getCurrentUsername();
        return registrationRepository.findByUsernameAndIsDeletedFalseOrderByRegisteredAtDesc(me)
                .stream()
                .filter(r -> !RSVP_CANCELLED.equals(r.getRsvpStatus()))
                .map(r -> {
                    String title = eventRepository.findById(r.getEventId())
                            .map(Event::getTitle).orElse(null);
                    return toRegDto(r, title);
                })
                .toList();
    }

    /** Cập nhật số lượng người đăng ký (GOING) lên sự kiện. */
    private void refreshRegisteredCount(Event e) {
        long going = registrationRepository
                .countByEventIdAndRsvpStatusAndIsDeletedFalse(e.getId(), RSVP_GOING);
        e.setRegisteredCount((int) going);
        eventRepository.save(e);
    }

    /** Gửi thông báo cho người tổ chức sự kiện. */
    private void notifyOrganizer(Event e, String title, String msg) {
        if (e.getOrganizerUsername() == null) return;
        try {
            notificationService.notify(
                    e.getOrganizerUsername(), title, msg,
                    "EVENT_STATUS", "EVENT", e.getId(),
                    "/admin/events?id=" + e.getId(),
                    SystemUtils.getCurrentUsername(), false);
        } catch (Exception ex) {
            log.warn("[event] notify failed: {}", ex.getMessage());
        }
    }

    /** Kiểm tra dữ liệu đầu vào khi tạo / cập nhật sự kiện (title và startAt bắt buộc). */
    private void validateSave(EventSaveRequest req) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "title bắt buộc");
        }
        if (req.getStartAt() == null || req.getStartAt().isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "startAt bắt buộc");
        }
    }

    /** Tìm sự kiện theo mã, ném ngoại lệ nếu không tồn tại hoặc đã bị xóa. */
    private Event find(String id) {
        return eventRepository.findById(id)
                .filter(e -> Boolean.FALSE.equals(e.getIsDeleted()) || e.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Event không tồn tại"));
    }

    /** Phân tích chuỗi thời gian ISO-8601, hỗ trợ cả định dạng ngày thuần túy. */
    private LocalDateTime parseDt(String raw) {
        try {
            return LocalDateTime.parse(raw.length() == 10 ? raw + "T00:00:00" : raw, ISO);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(raw);
            } catch (Exception e2) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, "Ngày giờ không hợp lệ: " + raw);
            }
        }
    }

    /** Chuyển entity sự kiện sang DTO, bao gồm số chỗ trống và trạng thái RSVP của người dùng. */
    private EventDto toDto(Event e, String myRsvp) {
        Integer seats = null;
        if (e.getCapacity() != null) {
            int reg = e.getRegisteredCount() != null ? e.getRegisteredCount() : 0;
            seats = Math.max(0, e.getCapacity() - reg);
        }
        return EventDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .location(e.getLocation())
                .startAt(e.getStartAt() != null ? e.getStartAt().format(ISO) : null)
                .endAt(e.getEndAt() != null ? e.getEndAt().format(ISO) : null)
                .status(e.getStatus())
                .capacity(e.getCapacity())
                .registeredCount(e.getRegisteredCount() != null ? e.getRegisteredCount() : 0)
                .seatsLeft(seats)
                .coverUrl(e.getCoverUrl())
                .organizerUsername(e.getOrganizerUsername())
                .publishedAt(e.getPublishedAt() != null ? e.getPublishedAt().format(ISO) : null)
                .cancelledAt(e.getCancelledAt() != null ? e.getCancelledAt().format(ISO) : null)
                .createdDate(e.getCreatedDate() != null ? e.getCreatedDate().format(ISO) : null)
                .myRsvpStatus(myRsvp)
                .build();
    }

    /** Chuyển entity đăng ký sự kiện sang DTO. */
    private EventRegistrationDto toRegDto(EventRegistration r, String eventTitle) {
        return EventRegistrationDto.builder()
                .id(r.getId())
                .eventId(r.getEventId())
                .eventTitle(eventTitle)
                .username(r.getUsername())
                .displayName(r.getDisplayName())
                .email(r.getEmail())
                .rsvpStatus(r.getRsvpStatus())
                .note(r.getNote())
                .registeredAt(r.getRegisteredAt() != null ? r.getRegisteredAt().format(ISO) : null)
                .build();
    }
}
