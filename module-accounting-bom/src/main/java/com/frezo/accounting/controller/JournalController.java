package com.frezo.accounting.controller;

import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.service.JournalService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounting/journals")
@RequiredArgsConstructor
@Tag(name = "Accounting - Journal", description = "Chứng từ ghi sổ")
public class JournalController {

    private final JournalService svc;

    @GetMapping("/{id}")
    @CheckPermission(api = "/accounting/journals/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(svc.getById(id));
    }

    @GetMapping
    @CheckPermission(api = "/accounting/journals", action = "VIEW")
    public ApiResponse<?> list(@RequestParam(required = false) String periodId,
                               @RequestParam(required = false) PostingSource source,
                               @RequestParam(required = false) String sourceId) {
        if (source != null && sourceId != null) {
            return ApiResponse.ok(svc.listBySource(source, sourceId));
        }
        if (periodId != null) {
            return ApiResponse.ok(svc.listByPeriod(periodId));
        }
        return ApiResponse.ok(java.util.List.of());
    }

    @PostMapping("/draft")
    @CheckPermission(api = "/accounting/journals/draft", action = "CREATE")
    @Operation(summary = "Tạo chứng từ DRAFT (chưa vào GL)")
    public ApiResponse<?> draft(@RequestBody @Valid JournalEntryRequest req) {
        return ApiResponse.created(svc.createDraft(req));
    }

    @PostMapping("/post")
    @CheckPermission(api = "/accounting/journals/post", action = "CREATE")
    @Operation(summary = "Tạo và POST luôn (idempotent theo idempotencyKey)")
    public ApiResponse<?> createAndPost(@RequestBody @Valid JournalEntryRequest req) {
        return ApiResponse.created(svc.createAndPost(req));
    }

    @PostMapping("/{id}/post")
    @CheckPermission(api = "/accounting/journals/{id}/post", action = "UPDATE")
    @Operation(summary = "Post DRAFT vào GL")
    public ApiResponse<?> post(@PathVariable String id) {
        return ApiResponse.ok(svc.post(id));
    }

    @PostMapping("/{id}/reverse")
    @CheckPermission(api = "/accounting/journals/{id}/reverse", action = "UPDATE")
    @Operation(summary = "Đảo chứng từ đã POSTED (tạo entry ngược)")
    public ApiResponse<?> reverse(@PathVariable String id,
                                  @RequestParam(required = false) String reason) {
        return ApiResponse.ok(svc.reverse(id, reason));
    }
}
