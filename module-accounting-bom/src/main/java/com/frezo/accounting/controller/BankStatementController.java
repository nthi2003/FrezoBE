package com.frezo.accounting.controller;

import com.frezo.accounting.dto.response.BankStatementDto;
import com.frezo.accounting.dto.response.BankStatementLineDto;
import com.frezo.accounting.dto.response.MatchSuggestionDto;
import com.frezo.accounting.service.BankStatementService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounting/bank-statements")
@RequiredArgsConstructor
@Tag(name = "Bank Reconciliation", description = "Import CSV + match journal lines")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    @GetMapping
    @CheckPermission(api = "/accounting/bank-statements", action = "VIEW")
    @Operation(summary = "Danh sách statement đã import")
    public ApiResponse<List<BankStatementDto>> list() {
        return ApiResponse.ok(bankStatementService.list());
    }

    @PostMapping("/import")
    @CheckPermission(api = "/accounting/bank-statements/import", action = "CREATE")
    @Operation(summary = "Import CSV sao kê")
    public ApiResponse<BankStatementDto> importCsv(
            @RequestParam String accountId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(bankStatementService.importCsv(accountId, file));
    }

    @GetMapping("/{id}/lines")
    @CheckPermission(api = "/accounting/bank-statements/{id}/lines", action = "VIEW")
    public ApiResponse<FePage<BankStatementLineDto>> lines(
            @PathVariable String id,
            @RequestParam(defaultValue = "all") String status) {
        return ApiResponse.ok(bankStatementService.listLines(id, status));
    }

    @GetMapping("/{id}/suggestions/{lineId}")
    @CheckPermission(api = "/accounting/bank-statements/{id}/suggestions/{lineId}", action = "VIEW")
    @Operation(summary = "Gợi ý match — mode=exact|fuzzy")
    public ApiResponse<List<MatchSuggestionDto>> suggestions(
            @PathVariable String id,
            @PathVariable String lineId,
            @RequestParam(defaultValue = "exact") String mode) {
        return ApiResponse.ok(bankStatementService.suggestions(id, lineId, mode));
    }

    @PostMapping("/lines/{lineId}/match")
    @CheckPermission(api = "/accounting/bank-statements/lines/{lineId}/match", action = "UPDATE")
    public ApiResponse<BankStatementLineDto> match(
            @PathVariable String lineId,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(bankStatementService.match(lineId, body.get("journalEntryLineId")));
    }

    @PostMapping("/lines/{lineId}/unmatch")
    @CheckPermission(api = "/accounting/bank-statements/lines/{lineId}/unmatch", action = "UPDATE")
    public ApiResponse<BankStatementLineDto> unmatch(@PathVariable String lineId) {
        return ApiResponse.ok(bankStatementService.unmatch(lineId));
    }

    @PostMapping("/{id}/lock")
    @CheckPermission(api = "/accounting/bank-statements/{id}/lock", action = "UPDATE")
    @Operation(summary = "Khoá statement — chặn match")
    public ApiResponse<BankStatementDto> lock(@PathVariable String id) {
        return ApiResponse.ok(bankStatementService.lock(id));
    }

    @PostMapping("/{id}/reopen")
    @CheckPermission(api = "/accounting/bank-statements/{id}/reopen", action = "UPDATE")
    @Operation(summary = "Mở lại statement")
    public ApiResponse<BankStatementDto> reopen(@PathVariable String id) {
        return ApiResponse.ok(bankStatementService.reopen(id));
    }
}
