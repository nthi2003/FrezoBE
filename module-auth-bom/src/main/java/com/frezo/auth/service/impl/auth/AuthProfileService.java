package com.frezo.auth.service.impl.auth;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cung cấp thông tin profile chi tiết cho user hiện tại (bao gồm Person / Contract active)
 * và xử lý upload avatar. Thao tác cross-module Person/Contract dùng reflection để
 * tránh dep cycle module-auth → qtht/qlns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthProfileService {

    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;
    private final MinioService minioService;

    /** Profile hiện tại — merge dữ liệu User + Person (reflection) + Contract active (reflection). */
    public Map<String, Object> getCurrentProfile() {
        String username = com.frezo.common.helper.SystemUtils.getCurrentUsername();
        if (username == null) {
            throw new AuthException("User not authenticated");
        }
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AuthException("User not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUserName());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("dataAction", user.getDataAction());

        if (user.getPersonId() != null) {
            profile.put("personId", user.getPersonId());
            enrichPerson(user.getPersonId(), profile);
            resolveActiveContract(user.getPersonId(), profile);
        }
        return profile;
    }

    /** Upload avatar từ file trên đĩa (dùng cho batch import). Trả về URL cuối cùng. */
    public String upload(File file, String username) {
        String extension = extractExtension(file.getName());
        String objectName = username + "/avatar" + extension;
        return minioService.uploadFileFromPath(objectName, file);
    }

    /** Upload avatar từ MultipartFile của web request; đồng thời lưu URL vào User. */
    public String uploadAvatar(MultipartFile file, String username) {
        try {
            String extension = extractExtension(file.getOriginalFilename());
            String objectName = "frezo-user/avatar/" + username + extension;
            String url = minioService.uploadFile(objectName, file);

            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new AuthException("User not found"));
            user.setAvatarUrl(url);
            userRepository.save(user);

            log.info("Avatar uploaded for user {}: {}", username, objectName);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload avatar for user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Không thể tải ảnh đại diện: " + e.getMessage());
        }
    }

    // ============================================================
    // Reflection helpers (cross-module không tạo compile-dep)
    // ============================================================

    private void enrichPerson(String personId, Map<String, Object> profile) {
        try {
            Class<?> personRepoClass = Class.forName("com.frezo.qtht.repository.PersonRepository");
            Object personRepo = applicationContext.getBean(personRepoClass);
            Method findById = personRepoClass.getMethod("findById", Object.class);
            Optional<?> personOpt = (Optional<?>) findById.invoke(personRepo, personId);
            if (personOpt.isPresent()) {
                Object person = personOpt.get();
                profile.put("phone", person.getClass().getMethod("getPhone").invoke(person));
                profile.put("jobTitle", person.getClass().getMethod("getJobTitle").invoke(person));
                profile.put("orgId", person.getClass().getMethod("getOrgId").invoke(person));
                profile.put("isAdmin", person.getClass().getMethod("getIsAdmin").invoke(person));
            }
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve person info for profile: {}", e.getMessage());
        }
    }

    /**
     * Tìm contract active gần nhất qua reflection để dùng cho Mobile leave-request feature.
     * Silent-fail nếu module-qlns chưa load — không phá login profile.
     */
    private void resolveActiveContract(String personId, Map<String, Object> profile) {
        try {
            Class<?> repoClass = Class.forName("com.frezo.qlns.repository.ContractRepository");
            Object repo = applicationContext.getBean(repoClass);
            Method findAll = repoClass.getMethod("findAll");
            Object result = findAll.invoke(repo);
            if (!(result instanceof Iterable<?> iter)) return;

            Object bestContract = null;
            java.time.LocalDate bestEffFrom = null;
            for (Object c : iter) {
                Object cPersonId = c.getClass().getMethod("getPersonId").invoke(c);
                if (!personId.equals(cPersonId)) continue;
                Object activated = c.getClass().getMethod("getActivated").invoke(c);
                if (activated != null && !((Boolean) activated)) continue;

                java.time.LocalDate effFrom = null;
                try {
                    Object v = c.getClass().getMethod("getEffFrom").invoke(c);
                    if (v instanceof java.time.LocalDate d) effFrom = d;
                } catch (NoSuchMethodException ignore) {}

                if (bestContract == null
                        || (effFrom != null && (bestEffFrom == null || effFrom.isAfter(bestEffFrom)))) {
                    bestContract = c;
                    bestEffFrom = effFrom;
                }
            }
            if (bestContract != null) {
                profile.put("contractId", bestContract.getClass().getMethod("getId").invoke(bestContract));
                tryPut(profile, "contractCode", bestContract, "getCode");
                tryPut(profile, "jobPosition", bestContract, "getJobPosition");
            }
        } catch (ClassNotFoundException e) {
            log.debug("[Auth] ContractRepository not found on classpath — skip contract resolve.");
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve active contract for personId={}: {}", personId, e.getMessage());
        }
    }

    private void tryPut(Map<String, Object> map, String key, Object target, String getter) {
        try {
            map.put(key, target.getClass().getMethod(getter).invoke(target));
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            log.debug("[Auth] Reflection getter {} failed: {}", getter, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        return filename.substring(filename.lastIndexOf("."));
    }
}
