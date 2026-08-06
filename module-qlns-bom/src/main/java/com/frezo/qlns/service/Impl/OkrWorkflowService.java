package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.qlns.dto.request.OkrWorkflowRequests;
import com.frezo.qlns.entity.Okr;
import com.frezo.qlns.entity.OkrKeyResult;
import com.frezo.qlns.entity.PerformanceCycle;
import com.frezo.qlns.repository.OkrKeyResultRepository;
import com.frezo.qlns.repository.OkrRepository;
import com.frezo.qlns.repository.PerformanceCycleRepository;
import com.frezo.qlns.service.impl.OkrScopeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OkrWorkflowService {
    private final JdbcTemplate jdbc;
    private final PerformanceCycleRepository cycleRepository;
    private final OkrRepository okrRepository;
    private final OkrKeyResultRepository keyResultRepository;
    private final OkrScopeResolver scopeResolver;

    public List<Map<String, Object>> listCycles() {
        return cycleRepository.findByIsDeletedFalseOrderByStartDateDesc().stream().map(this::cycleMap).toList();
    }

    @Transactional
    public Map<String, Object> createCycle(OkrWorkflowRequests.Cycle req) {
        assertAdmin();
        validateCycle(req);
        PerformanceCycle cycle = PerformanceCycle.builder()
                .name(req.getName().trim())
                .status(normalizeCycleStatus(req.getStatus()))
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();
        cycle.setId(UUID.randomUUID().toString());
        return cycleMap(cycleRepository.save(cycle));
    }

    @Transactional
    public Map<String, Object> updateCycle(String id, OkrWorkflowRequests.Cycle req) {
        assertAdmin();
        validateCycle(req);
        PerformanceCycle cycle = cycleRepository.findById(id)
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .orElseThrow(() -> notFound("Chu kỳ không tồn tại"));
        cycle.setName(req.getName().trim());
        cycle.setStatus(normalizeCycleStatus(req.getStatus()));
        cycle.setStartDate(req.getStartDate());
        cycle.setEndDate(req.getEndDate());
        return cycleMap(cycleRepository.save(cycle));
    }

    @Transactional
    public void deleteCycle(String id) {
        assertAdmin();
        PerformanceCycle cycle = cycleRepository.findById(id)
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .orElseThrow(() -> notFound("Chu kỳ không tồn tại"));
        cycle.setIsDeleted(true);
        cycleRepository.save(cycle);
    }

    public List<Map<String, Object>> listTimeline() {
        return jdbc.query("""
                SELECT id, step_name, department_name, time_label, detail, result, sort_order
                FROM okr_timeline_step WHERE is_deleted = false ORDER BY sort_order, created_date
                """, (rs, n) -> map(rs, "id", "stepName", "departmentName", "timeLabel", "detail", "result", "sortOrder"));
    }

    @Transactional
    public Map<String, Object> createTimeline(OkrWorkflowRequests.TimelineStep req) {
        assertAdmin();
        require(req.getStepName(), "Bước thực hiện là bắt buộc");
        String id = id();
        jdbc.update("""
                INSERT INTO okr_timeline_step
                (id, step_name, department_name, time_label, detail, result, sort_order, is_deleted, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP)
                """, id, req.getStepName().trim(), req.getDepartmentName(), req.getTimeLabel(), req.getDetail(),
                req.getResult(), req.getSortOrder() == null ? 0 : req.getSortOrder());
        return oneTimeline(id);
    }

    @Transactional
    public Map<String, Object> updateTimeline(String id, OkrWorkflowRequests.TimelineStep req) {
        assertAdmin();
        require(req.getStepName(), "Bước thực hiện là bắt buộc");
        int changed = jdbc.update("""
                UPDATE okr_timeline_step SET step_name=?, department_name=?, time_label=?, detail=?,
                result=?, sort_order=?, updated_date=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=false
                """, req.getStepName().trim(), req.getDepartmentName(), req.getTimeLabel(), req.getDetail(),
                req.getResult(), req.getSortOrder() == null ? 0 : req.getSortOrder(), id);
        if (changed == 0) throw notFound("Bước triển khai không tồn tại");
        return oneTimeline(id);
    }

    @Transactional
    public void deleteTimeline(String id) {
        assertAdmin();
        if (jdbc.update("UPDATE okr_timeline_step SET is_deleted=true WHERE id=? AND is_deleted=false", id) == 0) {
            throw notFound("Bước triển khai không tồn tại");
        }
    }

    public List<Map<String, Object>> listFeedbackTypes() {
        return jdbc.query("""
                SELECT id, name FROM okr_feedback_type WHERE is_deleted=false ORDER BY name
                """, (rs, n) -> map(rs, "id", "name"));
    }

    @Transactional
    public Map<String, Object> createFeedbackType(OkrWorkflowRequests.FeedbackType req) {
        assertAdmin();
        require(req.getName(), "Tên loại phiếu là bắt buộc");
        String id = id();
        try {
            jdbc.update("""
                    INSERT INTO okr_feedback_type (id, name, is_deleted, created_date)
                    VALUES (?, ?, false, CURRENT_TIMESTAMP)
                    """, id, req.getName().trim());
        } catch (RuntimeException ex) {
            throw new AppException(CommonErrorCode.CONFLICT, "Tên loại phiếu đã tồn tại");
        }
        return Map.of("id", id, "name", req.getName().trim());
    }

    @Transactional
    public Map<String, Object> updateFeedbackType(String id, OkrWorkflowRequests.FeedbackType req) {
        assertAdmin();
        require(req.getName(), "Tên loại phiếu là bắt buộc");
        if (jdbc.update("""
                UPDATE okr_feedback_type SET name=?, updated_date=CURRENT_TIMESTAMP
                WHERE id=? AND is_deleted=false
                """, req.getName().trim(), id) == 0) {
            throw notFound("Loại phiếu không tồn tại");
        }
        return Map.of("id", id, "name", req.getName().trim());
    }

    @Transactional
    public void deleteFeedbackType(String id) {
        assertAdmin();
        if (jdbc.update("UPDATE okr_feedback_type SET is_deleted=true WHERE id=? AND is_deleted=false", id) == 0) {
            throw notFound("Loại phiếu không tồn tại");
        }
    }

    public List<Map<String, Object>> listFeedback() {
        String me = currentPerson();
        if (scopeResolver.isAdmin() || scopeResolver.isManager(me)) {
            return jdbc.query("""
                    SELECT f.id, f.objective_id, f.target_scope, f.target_department_id, f.feedback_type_id,
                           t.name feedback_type_name, f.content, f.sender_person_id, f.created_date
                    FROM okr_feedback f JOIN okr_feedback_type t ON t.id=f.feedback_type_id
                    WHERE f.is_deleted=false ORDER BY f.created_date DESC
                    """, this::feedbackMap);
        }
        return jdbc.query("""
                SELECT f.id, f.objective_id, f.target_scope, f.target_department_id, f.feedback_type_id,
                       t.name feedback_type_name, f.content, f.sender_person_id, f.created_date
                FROM okr_feedback f JOIN okr_feedback_type t ON t.id=f.feedback_type_id
                WHERE f.is_deleted=false AND f.sender_person_id=? ORDER BY f.created_date DESC
                """, this::feedbackMap, me);
    }

    @Transactional
    public Map<String, Object> createFeedback(OkrWorkflowRequests.Feedback req) {
        require(req.getFeedbackTypeId(), "Loại phiếu là bắt buộc");
        require(req.getContent(), "Nội dung góp ý là bắt buộc");
        String target = req.getTargetScope() == null ? "COMPANY" : req.getTargetScope().trim().toUpperCase();
        if (!List.of("COMPANY", "DEPARTMENT").contains(target)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Phạm vi góp ý không hợp lệ");
        }
        if ("DEPARTMENT".equals(target)) require(req.getTargetDepartmentId(), "Phòng ban là bắt buộc");
        Integer typeCount = jdbc.queryForObject(
                "SELECT count(*) FROM okr_feedback_type WHERE id=? AND is_deleted=false",
                Integer.class, req.getFeedbackTypeId());
        if (typeCount == null || typeCount == 0) throw notFound("Loại phiếu không tồn tại");
        if (req.getObjectiveId() != null && !req.getObjectiveId().isBlank()) {
            scopeResolver.assertCanView(findOkr(req.getObjectiveId()).getOwnerPersonId());
        }
        String id = id();
        jdbc.update("""
                INSERT INTO okr_feedback
                (id, objective_id, target_scope, target_department_id, feedback_type_id, content,
                 sender_person_id, is_deleted, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP)
                """, id, blankToNull(req.getObjectiveId()), target, blankToNull(req.getTargetDepartmentId()),
                req.getFeedbackTypeId(), req.getContent().trim(), currentPerson());
        return listFeedback().stream().filter(x -> id.equals(x.get("id"))).findFirst().orElse(Map.of("id", id));
    }

    public List<Map<String, Object>> listActions(String keyResultId) {
        Okr okr = okrForKeyResult(keyResultId);
        scopeResolver.assertCanView(okr.getOwnerPersonId());
        return jdbc.query("""
                SELECT id, key_result_id, title, plan_url, start_date, end_date, result, status,
                       related_person_ids, created_date
                FROM okr_action WHERE key_result_id=? AND is_deleted=false ORDER BY created_date
                """, this::actionMap, keyResultId);
    }

    @Transactional
    public Map<String, Object> createAction(String keyResultId, OkrWorkflowRequests.Action req) {
        Okr okr = okrForKeyResult(keyResultId);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        validateAction(req);
        String id = id();
        jdbc.update("""
                INSERT INTO okr_action
                (id, key_result_id, title, plan_url, start_date, end_date, result, status,
                 related_person_ids, is_deleted, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP)
                """, id, keyResultId, req.getTitle().trim(), req.getPlanUrl(), req.getStartDate(), req.getEndDate(),
                req.getResult(), normalizeActionStatus(req.getStatus()), join(req.getRelatedPersonIds()));
        return oneAction(id);
    }

    @Transactional
    public Map<String, Object> updateAction(String id, OkrWorkflowRequests.Action req) {
        Map<String, Object> old = oneAction(id);
        Okr okr = okrForKeyResult((String) old.get("keyResultId"));
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        validateAction(req);
        jdbc.update("""
                UPDATE okr_action SET title=?, plan_url=?, start_date=?, end_date=?, result=?, status=?,
                related_person_ids=?, updated_date=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=false
                """, req.getTitle().trim(), req.getPlanUrl(), req.getStartDate(), req.getEndDate(), req.getResult(),
                normalizeActionStatus(req.getStatus()), join(req.getRelatedPersonIds()), id);
        return oneAction(id);
    }

    @Transactional
    public void deleteAction(String id) {
        Map<String, Object> old = oneAction(id);
        Okr okr = okrForKeyResult((String) old.get("keyResultId"));
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        jdbc.update("UPDATE okr_action SET is_deleted=true WHERE id=?", id);
    }

    public List<Map<String, Object>> listCheckIns(String okrId) {
        Okr okr = findOkr(okrId);
        scopeResolver.assertCanView(okr.getOwnerPersonId());
        List<Map<String, Object>> sessions = jdbc.query("""
                SELECT id, okr_id, employee_person_id, manager_person_id, progress, delayed_work, blockers,
                       solutions, confidence_level, status, official_update, manager_feedback,
                       next_check_in_date, complete_okrs, created_date, confirmed_at
                FROM okr_checkin_session WHERE okr_id=? AND is_deleted=false ORDER BY created_date DESC
                """, this::checkInMap, okrId);
        sessions.forEach(s -> s.put("feedback", listCheckInFeedback((String) s.get("id"))));
        return sessions;
    }

    @Transactional
    public Map<String, Object> createCheckIn(String okrId, OkrWorkflowRequests.CheckIn req) {
        Okr okr = findOkr(okrId);
        scopeResolver.assertCanModify(okr.getOwnerPersonId());
        require(req.getManagerPersonId(), "Quản lý xác nhận là bắt buộc");
        if (!scopeResolver.isAdmin() && !scopeResolver.subordinatePersonIds(req.getManagerPersonId()).contains(okr.getOwnerPersonId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Người xác nhận không quản lý nhân viên này");
        }
        if (req.getConfidenceLevel() == null || req.getConfidenceLevel() < 1 || req.getConfidenceLevel() > 5) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Mức độ tự tin phải từ 1 đến 5");
        }
        String id = id();
        jdbc.update("""
                INSERT INTO okr_checkin_session
                (id, okr_id, employee_person_id, manager_person_id, progress, delayed_work, blockers,
                 solutions, confidence_level, status, is_deleted, created_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', false, CURRENT_TIMESTAMP)
                """, id, okrId, okr.getOwnerPersonId(), req.getManagerPersonId(), req.getProgress(),
                req.getDelayedWork(), req.getBlockers(), req.getSolutions(), req.getConfidenceLevel());
        return oneCheckIn(id);
    }

    @Transactional
    public Map<String, Object> confirmCheckIn(String id, OkrWorkflowRequests.CheckIn req) {
        Map<String, Object> session = oneCheckIn(id);
        String me = currentPerson();
        if (!scopeResolver.isAdmin() && !me.equals(session.get("managerPersonId"))) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ quản lý được chọn mới xác nhận check-in");
        }
        String status = Boolean.TRUE.equals(req.getCompleteOkrs()) ? "COMPLETED" : "CONFIRMED";
        jdbc.update("""
                UPDATE okr_checkin_session SET status=?, official_update=?, manager_feedback=?,
                next_check_in_date=?, complete_okrs=?, confirmed_at=CURRENT_TIMESTAMP, updated_date=CURRENT_TIMESTAMP
                WHERE id=? AND is_deleted=false
                """, status, req.getOfficialUpdate(), req.getManagerFeedback(), req.getNextCheckInDate(),
                Boolean.TRUE.equals(req.getCompleteOkrs()), id);
        if ("COMPLETED".equals(status)) {
            jdbc.update("UPDATE okr SET status='COMPLETED' WHERE id=?", session.get("okrId"));
        }
        return oneCheckIn(id);
    }

    @Transactional
    public Map<String, Object> addCheckInFeedback(String checkInId, OkrWorkflowRequests.CheckInFeedback req) {
        require(req.getContent(), "Nội dung phản hồi là bắt buộc");
        Map<String, Object> session = oneCheckIn(checkInId);
        Okr okr = findOkr((String) session.get("okrId"));
        scopeResolver.assertCanView(okr.getOwnerPersonId());
        if (req.getParentFeedbackId() != null && !req.getParentFeedbackId().isBlank()) {
            Integer count = jdbc.queryForObject("""
                    SELECT count(*) FROM okr_checkin_feedback
                    WHERE id=? AND checkin_id=? AND is_deleted=false
                    """, Integer.class, req.getParentFeedbackId(), checkInId);
            if (count == null || count == 0) throw notFound("Phản hồi cha không tồn tại");
        }
        String id = id();
        jdbc.update("""
                INSERT INTO okr_checkin_feedback
                (id, checkin_id, parent_feedback_id, author_person_id, content, is_deleted, created_date)
                VALUES (?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP)
                """, id, checkInId, blankToNull(req.getParentFeedbackId()), currentPerson(), req.getContent().trim());
        return listCheckInFeedback(checkInId).stream()
                .filter(x -> id.equals(x.get("id"))).findFirst().orElse(Map.of("id", id));
    }

    private List<Map<String, Object>> listCheckInFeedback(String checkInId) {
        return jdbc.query("""
                SELECT id, parent_feedback_id, author_person_id, content, created_date
                FROM okr_checkin_feedback WHERE checkin_id=? AND is_deleted=false ORDER BY created_date
                """, (rs, n) -> map(rs, "id", "parentFeedbackId", "authorPersonId", "content", "createdDate"), checkInId);
    }

    private Map<String, Object> oneTimeline(String id) {
        return jdbc.query("""
                SELECT id, step_name, department_name, time_label, detail, result, sort_order
                FROM okr_timeline_step WHERE id=? AND is_deleted=false
                """, (rs, n) -> map(rs, "id", "stepName", "departmentName", "timeLabel", "detail", "result", "sortOrder"), id)
                .stream().findFirst().orElseThrow(() -> notFound("Bước triển khai không tồn tại"));
    }

    private Map<String, Object> oneAction(String id) {
        return jdbc.query("""
                SELECT id, key_result_id, title, plan_url, start_date, end_date, result, status,
                       related_person_ids, created_date
                FROM okr_action WHERE id=? AND is_deleted=false
                """, this::actionMap, id).stream().findFirst().orElseThrow(() -> notFound("Kế hoạch không tồn tại"));
    }

    private Map<String, Object> oneCheckIn(String id) {
        return jdbc.query("""
                SELECT id, okr_id, employee_person_id, manager_person_id, progress, delayed_work, blockers,
                       solutions, confidence_level, status, official_update, manager_feedback,
                       next_check_in_date, complete_okrs, created_date, confirmed_at
                FROM okr_checkin_session WHERE id=? AND is_deleted=false
                """, this::checkInMap, id).stream().findFirst().orElseThrow(() -> notFound("Check-in không tồn tại"));
    }

    private Map<String, Object> feedbackMap(ResultSet rs, int row) throws SQLException {
        return map(rs, "id", "objectiveId", "targetScope", "targetDepartmentId", "feedbackTypeId",
                "feedbackTypeName", "content", "senderPersonId", "createdDate");
    }

    private Map<String, Object> actionMap(ResultSet rs, int row) throws SQLException {
        Map<String, Object> result = map(rs, "id", "keyResultId", "title", "planUrl", "startDate",
                "endDate", "result", "status", "relatedPersonIds", "createdDate");
        Object related = result.get("relatedPersonIds");
        result.put("relatedPersonIds", related == null || related.toString().isBlank()
                ? List.of() : List.of(related.toString().split(",")));
        return result;
    }

    private Map<String, Object> checkInMap(ResultSet rs, int row) throws SQLException {
        return map(rs, "id", "okrId", "employeePersonId", "managerPersonId", "progress", "delayedWork",
                "blockers", "solutions", "confidenceLevel", "status", "officialUpdate", "managerFeedback",
                "nextCheckInDate", "completeOkrs", "createdDate", "confirmedAt");
    }

    private Map<String, Object> map(ResultSet rs, String... names) throws SQLException {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            Object value = rs.getObject(i + 1);
            if (value instanceof Date date) value = date.toLocalDate();
            out.put(names[i], value);
        }
        return out;
    }

    private Map<String, Object> cycleMap(PerformanceCycle cycle) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", cycle.getId());
        out.put("name", cycle.getName());
        out.put("status", cycle.getStatus());
        out.put("startDate", cycle.getStartDate());
        out.put("endDate", cycle.getEndDate());
        return out;
    }

    private void validateCycle(OkrWorkflowRequests.Cycle req) {
        require(req.getName(), "Tên chu kỳ là bắt buộc");
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Ngày bắt đầu và kết thúc là bắt buộc");
        }
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
    }

    private void validateAction(OkrWorkflowRequests.Action req) {
        require(req.getTitle(), "Tên kế hoạch là bắt buộc");
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
    }

    private String normalizeCycleStatus(String value) {
        String status = value == null ? "OPEN" : value.trim().toUpperCase();
        if (!List.of("OPEN", "CLOSED").contains(status)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Trạng thái chu kỳ không hợp lệ");
        }
        return status;
    }

    private String normalizeActionStatus(String value) {
        String status = value == null ? "TODO" : value.trim().toUpperCase();
        if (!List.of("TODO", "DOING", "DONE").contains(status)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, "Trạng thái kế hoạch không hợp lệ");
        }
        return status;
    }

    private Okr okrForKeyResult(String keyResultId) {
        OkrKeyResult kr = keyResultRepository.findById(keyResultId)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> notFound("Key Result không tồn tại"));
        return findOkr(kr.getOkrId());
    }

    private Okr findOkr(String id) {
        return okrRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> notFound("OKR không tồn tại"));
    }

    private String currentPerson() {
        return scopeResolver.currentPersonId()
                .orElseThrow(() -> new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự"));
    }

    private void assertAdmin() {
        if (!scopeResolver.isAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ quản trị viên được cấu hình OKR");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) throw new AppException(CommonErrorCode.VALIDATION_FAILED, message);
    }

    private AppException notFound(String message) {
        return new AppException(CommonErrorCode.NOT_FOUND, message);
    }

    private String id() {
        return UUID.randomUUID().toString();
    }

    private String join(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
