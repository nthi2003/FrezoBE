package com.frezo.qtht.config;

import com.frezo.common.security.CheckPermission;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.repository.PermissionRepository;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aspect kiểm tra {@code @CheckPermission(api, action)} cho MỌI endpoint annotate.
 * <p>
 * <b>v1.1 fixes (Batch C):</b>
 * <ul>
 *   <li>Unify exception: throw {@link AppException} với {@link CommonErrorCode#UNAUTHORIZED} /
 *       {@link CommonErrorCode#FORBIDDEN} thay {@code QTHTException} (deprecated).</li>
 *   <li>Feature toggle qua {@link SecurityPermissionProperties#isEnabled()} — cho phép rollback khẩn.</li>
 *   <li>{@code failOpenOnError} = false mặc định — Aspect gặp lỗi nội bộ → DENY (secure).</li>
 *   <li>Audit log rõ hơn: log cả allow và deny với user + api + action.</li>
 * </ul>
 * <p>
 * <b>Bypass rules (không đổi):</b>
 * <ul>
 *   <li>User có {@code Person.isAdmin = true} → SUPER_ADMIN, bypass toàn bộ check.</li>
 *   <li>User có {@code dataAction = 3} (Toàn quyền) → bypass.</li>
 * </ul>
 * <p>
 * <b>Rollout plan:</b> annotation {@code @CheckPermission} đang bị comment-out ở phần lớn controllers.
 * Sau khi aspect này ổn định, uncomment từng module — TRƯỚC ĐÓ đảm bảo bảng {@code permission} +
 * {@code role_permission} + {@code user_role} đã seed đầy đủ cho module đó (chạy migration seed).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckPermissionAspect {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final SecurityPermissionProperties props;

    @Before("@annotation(checkPermission)")
    public void validatePermission(JoinPoint joinPoint, CheckPermission checkPermission) {
        // -------- Feature toggle: cho phép rollback khẩn (chỉ dùng khi thật sự cần) --------
        if (!props.isEnabled()) {
            log.warn("CheckPermission is DISABLED via 'app.security.check-permission.enabled=false'. " +
                    "This is UNSAFE for production — re-enable ASAP.");
            return;
        }

        String userName = SystemUtils.getCurrentUsername();
        if (userName == null || userName.isEmpty() || "anonymousUser".equals(userName)) {
            throw new AppException(CommonErrorCode.UNAUTHORIZED);
        }

        // -------- Load user + bypass check --------
        Optional<User> userOpt;
        try {
            userOpt = userRepository.findByUserName(userName);
        } catch (Exception ex) {
            log.error("CheckPermission failed to load user '{}'", userName, ex);
            if (props.isFailOpenOnError()) return;
            throw new AppException(CommonErrorCode.FORBIDDEN, ex);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // SUPER_ADMIN bypass: Person.isAdmin = true
            boolean isAdmin = false;
            try {
                if (user.getPersonId() != null) {
                    isAdmin = personRepository.findByIdAndIsDeletedFalse(user.getPersonId())
                            .map(person -> Boolean.TRUE.equals(person.getIsAdmin()))
                            .orElse(false);
                } else if (user.getEmail() != null) {
                    isAdmin = personRepository.findByEmail(user.getEmail())
                            .map(person -> Boolean.TRUE.equals(person.getIsAdmin()))
                            .orElse(false);
                }
            } catch (Exception ex) {
                log.warn("CheckPermission failed to resolve isAdmin for user '{}': {}", userName, ex.getMessage());
            }

            if (isAdmin) {
                if (props.isLogAudit()) {
                    log.info("PERM-BYPASS SUPER_ADMIN user={} api={} action={} method={}",
                            userName, checkPermission.api(), checkPermission.action(), methodName(joinPoint));
                }
                return;
            }

            // dataAction bypass: 1=Nội bộ, 2=Cấp cha con, 3=Toàn quyền
            Short dataAction = user.getDataAction();
            if (dataAction != null && dataAction == 3) {
                if (props.isLogAudit()) {
                    log.info("PERM-BYPASS FULL_ACCESS(dataAction=3) user={} api={} action={} method={}",
                            userName, checkPermission.api(), checkPermission.action(), methodName(joinPoint));
                }
                return;
            }
        }

        // -------- Actual permission check --------
        String apiPath = checkPermission.api();
        String action = checkPermission.action();

        String groupCode = getGroupCodeFromArgs(joinPoint);
        if (groupCode != null && !groupCode.isEmpty()) {
            apiPath = apiPath + "/" + groupCode;
        }

        boolean hasPermission;
        try {
            hasPermission = permissionRepository.checkPermission(userName, apiPath, action);
        } catch (Exception ex) {
            log.error("CheckPermission query failed user={} api={} action={}", userName, apiPath, action, ex);
            if (props.isFailOpenOnError()) return;
            throw new AppException(CommonErrorCode.FORBIDDEN, ex);
        }

        if (!hasPermission) {
            log.warn("PERM-DENY user={} api={} action={} method={}",
                    userName, apiPath, action, methodName(joinPoint));
            throw new AppException(CommonErrorCode.FORBIDDEN, apiPath, action);
        }

        if (props.isLogAudit()) {
            log.info("PERM-ALLOW user={} api={} action={} method={}",
                    userName, apiPath, action, methodName(joinPoint));
        }
    }

    private String getGroupCodeFromArgs(JoinPoint joinPoint) {
        if (joinPoint.getArgs() == null) return null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null) continue;
            try {
                Method method = arg.getClass().getMethod("getGroupCode");
                Object result = method.invoke(arg);
                if (result != null) return result.toString();
            } catch (Exception ignored) {
                // no-op: arg không có getGroupCode
            }
        }
        return null;
    }

    private String methodName(JoinPoint jp) {
        return jp.getSignature().toShortString();
    }
}
