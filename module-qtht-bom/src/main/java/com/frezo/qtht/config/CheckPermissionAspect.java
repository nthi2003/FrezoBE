package com.frezo.qtht.config;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.repository.PermissionRepository;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckPermissionAspect {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    @Before("@annotation(checkPermission)")
    public void validatePermission(JoinPoint joinPoint, CheckPermission checkPermission) {
        String userName = SystemUtils.getCurrentUsername();
        if (userName == null || userName.isEmpty() || "anonymousUser".equals(userName)) {
            throw new QTHTException("user.is.not.auth");
        }

        Optional<User> userOpt = userRepository.findByUserName(userName);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if user is admin
            boolean isAdmin = false;
            if (user.getPersonId() != null) {
                isAdmin = personRepository.findByIdAndIsDeletedFalse(user.getPersonId())
                        .map(person -> Boolean.TRUE.equals(person.getIsAdmin()))
                        .orElse(false);
            } else if (user.getEmail() != null) {
                isAdmin = personRepository.findByEmail(user.getEmail())
                        .map(person -> Boolean.TRUE.equals(person.getIsAdmin()))
                        .orElse(false);
            }

            if (isAdmin) {
                log.debug("Person.isAdmin = true, user {} bypass permission check", userName);
                return;
            }

            // Check dataAction scope
            Short dataAction = user.getDataAction();
            log.debug("User {} has dataAction: {}", userName, dataAction);

            // dataAction: 1=Nội bộ, 2=Cấp cha con, 3=Toàn quyền
            if (dataAction != null && dataAction == 3) {
                log.debug("User {} has full permission (dataAction=3), bypass permission check", userName);
                return;
            }
        }

        String apiPath = checkPermission.api();
        String action = checkPermission.action();

        String groupCode = getGroupCodeFromArgs(joinPoint);
        if (groupCode != null && !groupCode.isEmpty()) {
            apiPath = apiPath + "/" + groupCode;
        }

        log.debug("Checking permission for user: {}, api: {}, action: {}", userName, apiPath, action);

        boolean hasPermission = permissionRepository.checkPermission(userName, apiPath, action);

        if (!hasPermission) {
            log.warn("Access Denied for user: {} on api: {} action: {}", userName, apiPath, action);
            throw new QTHTException("error.access.denied", apiPath, action);
        }
    }

    private String getGroupCodeFromArgs(JoinPoint joinPoint) {
        if (joinPoint.getArgs() == null)
            return null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg == null)
                continue;
            try {
                java.lang.reflect.Method method = arg.getClass().getMethod("getGroupCode");
                Object result = method.invoke(arg);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception e) {

            }
        }
        return null;
    }
}
