package com.frezo.qtht.service.impl;

import com.frezo.qtht.job.DatabaseBackupJob;
import com.frezo.qtht.service.SystemAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemAdminServiceImpl implements SystemAdminService {

    private final DatabaseBackupJob databaseBackupJob;

    @Override
    public void triggerBackup() {
        databaseBackupJob.executeBackup();
    }
}
