package com.frezo.qtbv.service;

import com.frezo.qtbv.entity.LandingConfig;

public interface LandingConfigService {
    LandingConfig getConfig();
    LandingConfig updateConfig(LandingConfig config);
}
