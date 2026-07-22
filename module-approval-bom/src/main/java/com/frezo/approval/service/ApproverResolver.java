package com.frezo.approval.service;

import com.frezo.auth.entity.User;
import com.frezo.auth.entity.UserRole;
import com.frezo.auth.repository.UserRepository;
import com.frezo.auth.repository.UserRoleRepository;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolve approver từ role code → danh sách username/person.
 * Tách khỏi ApprovalService để giữ ≤5 deps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApproverResolver {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public record ApproverHint(String username, String personId, String displayName, String roleCode) {}

    /** Lấy user đầu tiên thuộc role (MVP — không pick cụ thể). */
    public Optional<ApproverHint> resolveFirst(String roleCode) {
        List<ApproverHint> all = resolveAll(roleCode);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    public List<ApproverHint> resolveAll(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return List.of();
        List<Role> roles = roleRepository.findByIsDeletedFalse().stream()
                .filter(r -> roleCode.equalsIgnoreCase(r.getCode()))
                .toList();
        List<ApproverHint> out = new ArrayList<>();
        for (Role role : roles) {
            for (UserRole ur : userRoleRepository.findByRoleIdAndIsDeletedFalse(role.getId())) {
                userRepository.findById(ur.getUserId()).ifPresent(u -> {
                    if (u.getStatus() != null && u.getStatus() == 0) return;
                    out.add(new ApproverHint(
                            u.getUserName(),
                            u.getPersonId(),
                            u.getName() != null ? u.getName() : u.getUserName(),
                            roleCode));
                });
            }
        }
        return out;
    }

    public boolean userHasRole(String username, String roleCode) {
        if (username == null || roleCode == null) return false;
        Optional<User> userOpt = userRepository.findByUserName(username);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        List<UserRole> urs = userRoleRepository.findByUserIdAndIsDeletedFalse(user.getId());
        for (UserRole ur : urs) {
            Optional<Role> role = roleRepository.findById(ur.getRoleId());
            if (role.isPresent() && roleCode.equalsIgnoreCase(role.get().getCode())) {
                return true;
            }
        }
        // Fallback: username trùng role code (demo CFO/HR accounts)
        return username.equalsIgnoreCase(roleCode)
                || username.toLowerCase().contains(roleCode.toLowerCase());
    }

    public String displayName(String username) {
        if (username == null) return null;
        return userRepository.findByUserName(username)
                .map(u -> u.getName() != null ? u.getName() : u.getUserName())
                .orElse(username);
    }
}
