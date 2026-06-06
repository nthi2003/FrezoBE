package com.frezo.qtht.config;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class AuditAwareConfig implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        log.info("getCurrentAuditor");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail)) {
            return Optional.empty();
        }
        CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();
        User user = userDetail.getUser();
        log.info("getCurrentAuditor : {}", user.getId());
        return Optional.of(user.getId());
    }
}


