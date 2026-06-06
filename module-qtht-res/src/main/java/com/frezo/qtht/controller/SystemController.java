package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.job.DatabaseBackupJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.frezo.qtht.config.CheckPermission;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qtht/system")
@RequiredArgsConstructor
@Tag(name = "9. Quản trị hệ thống", description = "API quản trị hệ thống, backup dữ liệu")
public class SystemController {

    private final DatabaseBackupJob databaseBackupJob;

    @Operation(summary = "Backup dữ liệu thủ công", description = "Sao lưu DB và tải lên Google Drive ngay lập tức")
    @PostMapping("/backup")
    @CheckPermission(api = "/qtht/system/backup", action = "EXECUTE")
    public ApiResponse<?> triggerBackup() {
            databaseBackupJob.executeBackup();
            return ApiResponse.success("Bắt đầu quá trình backup dữ liệu. Vui lòng kiểm tra Google Drive sau ít phút.");
    }
}
