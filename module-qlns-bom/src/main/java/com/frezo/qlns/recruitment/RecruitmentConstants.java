package com.frezo.qlns.recruitment;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tập trung mọi hằng string dùng trong tính năng Recruitment ATS —
 * tránh magic strings rải rác trong service.
 */
public final class RecruitmentConstants {

    private RecruitmentConstants() {}

    // ---------------- Requisition status ----------------
    public static final String REQ_OPEN = "OPEN";
    public static final String REQ_ON_HOLD = "ON_HOLD";
    public static final String REQ_FILLED = "FILLED";
    public static final String REQ_CLOSED = "CLOSED";

    // ---------------- Application stage ----------------
    public static final String STAGE_APPLIED = "APPLIED";
    public static final String STAGE_SCREENING = "SCREENING";
    public static final String STAGE_INTERVIEW = "INTERVIEW";
    public static final String STAGE_OFFER = "OFFER";
    public static final String STAGE_HIRED = "HIRED";
    public static final String STAGE_REJECTED = "REJECTED";

    /**
     * Bảng chuyển stage hợp lệ (adjacency list).
     * <ul>
     *   <li>APPLIED → SCREENING | REJECTED</li>
     *   <li>SCREENING → INTERVIEW | REJECTED</li>
     *   <li>INTERVIEW → OFFER | REJECTED</li>
     *   <li>OFFER → HIRED | REJECTED (HIRED = ứng viên nhận offer)</li>
     * </ul>
     * HIRED / REJECTED là trạng thái cuối — không cho chuyển tiếp.
     */
    public static final Map<String, Set<String>> STAGE_TRANSITIONS = Map.of(
            STAGE_APPLIED, Set.of(STAGE_SCREENING, STAGE_REJECTED),
            STAGE_SCREENING, Set.of(STAGE_INTERVIEW, STAGE_REJECTED),
            STAGE_INTERVIEW, Set.of(STAGE_OFFER, STAGE_REJECTED),
            STAGE_OFFER, Set.of(STAGE_HIRED, STAGE_REJECTED)
    );

    public static final List<String> STAGES = List.of(
            STAGE_APPLIED, STAGE_SCREENING, STAGE_INTERVIEW,
            STAGE_OFFER, STAGE_HIRED, STAGE_REJECTED
    );

    // ---------------- Interview types & status ----------------
    public static final Set<String> INTERVIEW_TYPES = Set.of(
            "PHONE", "ONLINE", "ONSITE", "TECHNICAL", "HR", "FINAL"
    );

    public static final String INTV_SCHEDULED = "SCHEDULED";
    public static final String INTV_DONE = "DONE";
    public static final String INTV_CANCELLED = "CANCELLED";
    public static final String INTV_NO_SHOW = "NO_SHOW";

    // ---------------- Offer status ----------------
    public static final String OFFER_DRAFT = "DRAFT";
    public static final String OFFER_SENT = "SENT";
    public static final String OFFER_ACCEPTED = "ACCEPTED";
    public static final String OFFER_REJECTED = "REJECTED";
    public static final String OFFER_EXPIRED = "EXPIRED";
}
