package com.frezo.qtht.config;

import com.frezo.common.helper.SystemUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditAwareConfig implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SystemUtils.getCurrentUsername());
    }
}


