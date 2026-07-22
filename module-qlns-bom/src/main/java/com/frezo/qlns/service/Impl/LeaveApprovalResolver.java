package com.frezo.qlns.service.impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Resolve người duyệt cho workflow nghỉ phép.
 *
 * <h3>Chuỗi ưu tiên tìm QL trực tiếp</h3>
 * <ol>
 *   <li>{@code Person(personId).department.managerId} → {@code User.personId} → username</li>
 *   <li>Nếu Department không có managerId → thử {@code deputyManagerId}</li>
 *   <li>Nếu vẫn không → fallback null, caller sẽ chuyển thẳng sang HR</li>
 * </ol>
 *
 * <h3>Danh sách HR</h3>
 * Đọc từ config {@code frezo.leave.hr-users} (comma-separated usernames).
 * Default: {@code admin} (dev-friendly). Prod nên set qua env:
 * <pre>LEAVE_HR_USERS=hr_hoa,hr_lan,hr_minh</pre>
 *
 * <h3>Tại sao không dùng Role/Permission?</h3>
 * Ban đầu chọn cấu hình tường minh vì:
 * <ul>
 *   <li>Không phụ thuộc vào schema role hiện tại (chưa có "HR" role code chuẩn hoá)</li>
 *   <li>Dễ debug ở prod: chỉ cần đọc env là biết ai được ghim notification</li>
 *   <li>Migrate sang role-based sau này không phá API</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveApprovalResolver {

    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Value("${frezo.leave.hr-users:admin}")
    private String hrUsersRaw;

    /**
     * Tìm username QL trực tiếp của nhân viên (theo departmentId → managerId → User).
     *
     * @return username hoặc {@code null} nếu không xác định được
     *         (VD: nhân viên chưa có Person / chưa gán department / dept chưa có manager)
     */
    public String resolveManagerUsername(String personId) {
        if (personId == null || personId.isBlank()) return null;

        Person person = personRepository.findByIdAndIsDeletedFalse(personId).orElse(null);
        if (person == null) {
            log.warn("[leave-approval] Person không tồn tại personId={}", personId);
            return null;
        }
        String deptId = person.getDepartmentId();
        if (deptId == null || deptId.isBlank()) {
            log.warn("[leave-approval] Person chưa gán department personId={}", personId);
            return null;
        }

        Department dept = departmentRepository.findByIdAndIsDeletedFalse(deptId).orElse(null);
        if (dept == null) return null;

        // Ưu tiên manager, sau đó deputy
        String managerPersonId = dept.getManagerId() != null ? dept.getManagerId() : dept.getDeputyManagerId();
        if (managerPersonId == null || managerPersonId.isBlank()) {
            log.warn("[leave-approval] Department {} chưa gán manager/deputy", dept.getCode());
            return null;
        }
        // Người xin trùng chính là manager → skip cấp này (sẽ chuyển thẳng sang HR)
        if (managerPersonId.equals(personId)) {
            log.info("[leave-approval] Requester là manager của chính mình → skip cấp QL");
            return null;
        }

        return userRepository.findByPersonId(managerPersonId)
                .map(u -> u.getUserName())
                .orElseGet(() -> {
                    log.warn("[leave-approval] Manager personId={} chưa có User account", managerPersonId);
                    return null;
                });
    }

    /** Danh sách username HR — nhận thông báo ở step 2. Không rỗng nếu config đúng. */
    public List<String> resolveHrUsernames() {
        if (hrUsersRaw == null || hrUsersRaw.isBlank()) return Collections.emptyList();
        return Arrays.stream(hrUsersRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Kiểm tra current user có phải admin không.
     * Nguồn ground truth: {@code Person.isAdmin} (không dùng JWT claim để tránh
     * bị stale khi admin flag đổi giữa 2 lần login).
     */
    public boolean isCurrentUserAdmin() {
        String username = com.frezo.common.helper.SystemUtils.getCurrentUsername();
        if (username == null) return false;
        return userRepository.findByUserName(username)
                .map(u -> u.getPersonId())
                .flatMap(personId -> personRepository.findByIdAndIsDeletedFalse(personId))
                .map(Person::getIsAdmin)
                .orElse(false);
    }
}
