package com.frezo.qlns.service.impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Phạm vi xem/sửa OKR theo org hierarchy — khớp pattern LeaveApprovalResolver.
 * <ul>
 *   <li>Nhân viên: chỉ OKR của mình</li>
 *   <li>QL (manager/deputy của phòng): OKR cấp dưới trong phòng + phòng con</li>
 *   <li>Admin: toàn bộ</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OkrScopeResolver {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;

    public Optional<String> currentPersonId() {
        String username = SystemUtils.getCurrentUsername();
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUserName(username)
                .map(u -> u.getPersonId())
                .filter(id -> id != null && !id.isBlank());
    }

    public boolean isAdmin() {
        return currentPersonId()
                .flatMap(personRepository::findByIdAndIsDeletedFalse)
                .map(p -> Boolean.TRUE.equals(p.getIsAdmin()))
                .orElse(false);
    }

    /** Có ít nhất một phòng ban gán manager/deputy = personId. */
    public boolean isManager(String personId) {
        if (personId == null || personId.isBlank()) return false;
        return !departmentRepository.findManagedByPersonId(personId).isEmpty();
    }

    /** Person IDs cấp dưới (phòng QL trực tiếp + phòng con), không gồm chính manager. */
    public List<String> subordinatePersonIds(String managerPersonId) {
        if (managerPersonId == null || managerPersonId.isBlank()) return List.of();

        List<Department> managed = departmentRepository.findManagedByPersonId(managerPersonId);
        if (managed.isEmpty()) return List.of();

        Set<String> deptIds = new HashSet<>();
        for (Department dept : managed) {
            collectDeptTreeIds(dept, deptIds);
        }
        if (deptIds.isEmpty()) return List.of();

        return personRepository.findActivePersonIdsByDepartmentIds(deptIds, managerPersonId);
    }

    public void assertScopeAllowed(String scope, String currentPersonId) {
        boolean admin = isAdmin();
        boolean manager = isManager(currentPersonId);
        String s = normalizeScope(scope);
        if ("all".equals(s) && !admin) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ admin mới xem toàn bộ OKR");
        }
        if ("team".equals(s) && !admin && !manager) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Bạn không có quyền xem OKR team");
        }
    }

    public void assertCanView(String ownerPersonId) {
        String me = currentPersonId().orElse(null);
        if (me == null) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự");
        }
        if (isAdmin() || me.equals(ownerPersonId)) return;
        if (subordinatePersonIds(me).contains(ownerPersonId)) return;
        throw new AppException(CommonErrorCode.FORBIDDEN, "Không có quyền xem OKR này");
    }

    /** Chỉ owner hoặc admin được tạo/sửa/check-in. Manager team chỉ xem. */
    public void assertCanModify(String ownerPersonId) {
        String me = currentPersonId().orElse(null);
        if (me == null) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự");
        }
        if (isAdmin() || me.equals(ownerPersonId)) return;
        throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ chủ OKR hoặc admin được cập nhật");
    }

    public void assertCanAssignOwner(String targetOwnerPersonId) {
        String me = currentPersonId().orElse(null);
        if (me == null) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự");
        }
        if (targetOwnerPersonId == null || targetOwnerPersonId.isBlank()) return;
        if (isAdmin() || me.equals(targetOwnerPersonId)) return;
        if (subordinatePersonIds(me).contains(targetOwnerPersonId)) return;
        throw new AppException(CommonErrorCode.FORBIDDEN, "Không thể gán OKR cho nhân viên ngoài phạm vi");
    }

    /**
     * Quyền công bố được kiểm tra theo đúng phạm vi mục tiêu, không dựa vào nút FE.
     * Admin đại diện cấp công ty; manager chỉ được công bố team/phòng mình quản lý.
     */
    public void assertCanPublish(String scopeType, String ownerPersonId, String departmentId) {
        String me = currentPersonId().orElse(null);
        if (me == null) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự");
        }
        String scope = scopeType == null ? "PERSONAL" : scopeType.trim().toUpperCase();
        if (isAdmin()) return;
        if ("PERSONAL".equals(scope)) {
            if (me.equals(ownerPersonId)) return;
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ chủ OKR được công bố mục tiêu cá nhân");
        }
        if (!isManager(me)) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ quản lý được công bố mục tiêu theo đơn vị");
        }
        if ("TEAM".equals(scope) && subordinatePersonIds(me).contains(ownerPersonId)) return;
        if ("DEPARTMENT".equals(scope) && departmentId != null
                && departmentRepository.findManagedByPersonId(me).stream()
                .anyMatch(d -> departmentId.equals(d.getId())
                        || (d.getPath() != null && departmentRepository
                        .findByPathStartingWithAndIsDeletedFalse(d.getPath()).stream()
                        .anyMatch(child -> departmentId.equals(child.getId()))))) {
            return;
        }
        throw new AppException(CommonErrorCode.FORBIDDEN, "Không được công bố OKR ngoài phạm vi quản lý");
    }

    public static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) return "mine";
        return scope.trim().toLowerCase();
    }

    private void collectDeptTreeIds(Department root, Set<String> out) {
        if (root == null || root.getId() == null) return;
        out.add(root.getId());
        String path = root.getPath();
        if (path != null && !path.isBlank()) {
            departmentRepository.findByPathStartingWithAndIsDeletedFalse(path)
                    .forEach(d -> out.add(d.getId()));
        }
    }
}
