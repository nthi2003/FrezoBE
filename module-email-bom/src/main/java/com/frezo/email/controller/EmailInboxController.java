package com.frezo.email.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.email.dto.response.EmailInboxResponse;
import com.frezo.email.service.EmailInboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/email/inbox")
@RequiredArgsConstructor
@Tag(name = "Email Inbox", description = "APIs for reading emails from mailbox (IMAP)")
public class EmailInboxController {

    private final EmailInboxService emailInboxService;

    @Operation(summary = "Lấy danh sách email trong hộp thư", description = "folder: inbox, sent, drafts, trash, starred, spam")
    @GetMapping("/{configId}")
    public ResponseEntity<ApiResponse<List<EmailInboxResponse>>> getInbox(
            @PathVariable String configId,
            @RequestParam(defaultValue = "inbox") String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(emailInboxService.fetchInbox(configId, folder, page, size)));
    }

    @Operation(summary = "Xem chi tiết một email")
    @GetMapping("/{configId}/{uid}")
    public ResponseEntity<ApiResponse<EmailInboxResponse>> getEmail(
            @PathVariable String configId,
            @PathVariable long uid) {
        return ResponseEntity.ok(ApiResponse.success(emailInboxService.fetchEmailById(configId, uid)));
    }

    @Operation(summary = "Đánh dấu email đã đọc")
    @PutMapping("/{configId}/{uid}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String configId,
            @PathVariable long uid) {
        emailInboxService.markAsRead(configId, uid);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
