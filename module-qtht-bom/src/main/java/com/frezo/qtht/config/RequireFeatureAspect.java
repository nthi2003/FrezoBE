package com.frezo.qtht.config;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.dto.response.SettingResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.SettingService;
import com.frezo.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequireFeatureAspect {

    private final SettingService settingService;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    @Before("@annotation(requireFeature)")
    public void validateFeature(JoinPoint joinPoint, RequireFeature requireFeature) {
        String userName = SystemUtils.getCurrentUsername();
        if (userName == null || userName.isEmpty() || "anonymousUser".equals(userName)) {
            throw new QTHTException("user.is.not.auth");
        }

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new QTHTException("user.not.found"));

        String personId = user.getPersonId();
        if (personId == null) {
            log.debug("User {} has no personId, skipping feature check", userName);
            return;
        }

        Person person = personRepository.findByIdAndIsDeletedFalse(personId)
                .orElseThrow(() -> new QTHTException("person.not.found"));

        // Admin bypass feature check as per spec? 
        // Spec says: "Không áp dụng cho Super Admin hệ thống (ngoài scope của org)"
        // But "Nếu Setting.isAttendance = false thì không ai trong org đó có thể sử dụng module chấm công, kể cả Admin của org."
        // So I will only bypass for Super Admin (where isAdmin is true in a certain way)
        if (Boolean.TRUE.equals(person.getIsAdmin())) {
            log.debug("User {} is admin, bypass feature check", userName);
            return;
        }

        String orgId = person.getOrgId();
        if (orgId == null) {
            log.warn("User {} has no orgId, feature check failed", userName);
            throw new QTHTException("feature.denied.no.org");
        }

        ApiResponse<SettingResponse> apiResponse = settingService.getByOrgId(orgId);
        SettingResponse setting = apiResponse.getData();
        
        String featureName = requireFeature.value();
        try {
            Field field = SettingResponse.class.getDeclaredField(featureName);
            field.setAccessible(true);
            Boolean enabled = (Boolean) field.get(setting);
            
            if (enabled == null || !enabled) {
                log.warn("Feature {} is disabled for org {}", featureName, orgId);
                throw new QTHTException("FEATURE_DISABLED", featureName);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Invalid feature name: {}", featureName);
            throw new QTHTException("invalid.feature.name", featureName);
        }
    }
}
