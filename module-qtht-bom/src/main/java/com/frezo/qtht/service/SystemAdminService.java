package com.frezo.qtht.service;

public interface SystemAdminService {

    /** Backup database thủ công (fire-and-forget). */
    void triggerBackup();
}
