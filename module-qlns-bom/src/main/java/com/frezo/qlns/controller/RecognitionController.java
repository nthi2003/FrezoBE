package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.TokenGiftRequest;
import com.frezo.qlns.dto.request.TokenRedeemCreateRequest;
import com.frezo.qlns.dto.request.TokenRedeemReviewRequest;
import com.frezo.qlns.dto.response.RecognitionConfigResponse;
import com.frezo.qlns.dto.response.TokenRedeemResponse;
import com.frezo.qlns.dto.response.TokenRewardCatalogResponse;
import com.frezo.qlns.dto.response.TokenTransferResponse;
import com.frezo.qlns.dto.response.TokenWalletResponse;
import com.frezo.qlns.service.RecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qlns/recognition")
@RequiredArgsConstructor
@Tag(name = "QLNS — Ghi nhận / Token", description = "Tặng token · đổi thưởng · duyệt kế toán")
public class RecognitionController {

    private final RecognitionService recognitionService;

    @Operation(summary = "Cấu hình token (tỷ giá VND, max gift…)")
    @GetMapping("/config")
    @CheckPermission(api = "/qlns/recognition/config", action = "VIEW")
    public ApiResponse<RecognitionConfigResponse> config() {
        return ApiResponse.ok(recognitionService.getConfig());
    }

    @Operation(summary = "Ví token của tôi")
    @GetMapping("/wallet/me")
    @CheckPermission(api = "/qlns/recognition/wallet/me", action = "VIEW")
    public ApiResponse<TokenWalletResponse> myWallet() {
        return ApiResponse.ok(recognitionService.getMyWallet());
    }

    @Operation(summary = "Ví token theo personId")
    @GetMapping("/wallet/{personId}")
    @CheckPermission(api = "/qlns/recognition/wallet/{personId}", action = "VIEW")
    public ApiResponse<TokenWalletResponse> wallet(@PathVariable String personId) {
        return ApiResponse.ok(recognitionService.getWallet(personId));
    }

    @Operation(summary = "Danh sách ví (HR/Admin)")
    @GetMapping("/wallets")
    @CheckPermission(api = "/qlns/recognition/wallets", action = "VIEW")
    public ApiResponse<List<TokenWalletResponse>> wallets() {
        return ApiResponse.ok(recognitionService.listWallets());
    }

    @Operation(summary = "Tặng token")
    @PostMapping("/gift")
    @CheckPermission(api = "/qlns/recognition/gift", action = "CREATE")
    public ApiResponse<TokenTransferResponse> gift(@RequestBody TokenGiftRequest request) {
        return ApiResponse.ok(recognitionService.gift(request));
    }

    @Operation(summary = "Lịch sử chuyển token")
    @GetMapping("/transfers")
    @CheckPermission(api = "/qlns/recognition/transfers", action = "VIEW")
    public ApiResponse<List<TokenTransferResponse>> transfers(
            @RequestParam(required = false) String personId) {
        return ApiResponse.ok(recognitionService.listTransfers(personId));
    }

    @Operation(summary = "Tạo yêu cầu đổi thưởng")
    @PostMapping("/redeem")
    @CheckPermission(api = "/qlns/recognition/redeem", action = "CREATE")
    public ApiResponse<TokenRedeemResponse> redeem(@RequestBody TokenRedeemCreateRequest request) {
        return ApiResponse.ok(recognitionService.requestRedeem(request));
    }

    @Operation(summary = "Danh sách yêu cầu đổi thưởng")
    @GetMapping("/redeem")
    @CheckPermission(api = "/qlns/recognition/redeem", action = "VIEW")
    public ApiResponse<List<TokenRedeemResponse>> listRedeem(
            @RequestParam(required = false) String personId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(recognitionService.listRedeems(personId, status));
    }

    @Operation(summary = "Duyệt đổi thưởng → xếp vào kỳ lương hiện tại")
    @PostMapping("/redeem/{id}/approve")
    @CheckPermission(api = "/qlns/recognition/redeem/{id}/approve", action = "APPROVE")
    public ApiResponse<TokenRedeemResponse> approve(@PathVariable String id) {
        return ApiResponse.ok(recognitionService.approveRedeem(id));
    }

    @Operation(summary = "Từ chối đổi thưởng (hoàn token)")
    @PostMapping("/redeem/{id}/reject")
    @CheckPermission(api = "/qlns/recognition/redeem/{id}/reject", action = "APPROVE")
    public ApiResponse<TokenRedeemResponse> reject(
            @PathVariable String id, @RequestBody(required = false) TokenRedeemReviewRequest request) {
        return ApiResponse.ok(recognitionService.rejectRedeem(id, request));
    }

    @Operation(summary = "Catalog thưởng (optional)")
    @GetMapping("/catalog")
    @CheckPermission(api = "/qlns/recognition/catalog", action = "VIEW")
    public ApiResponse<List<TokenRewardCatalogResponse>> catalog() {
        return ApiResponse.ok(recognitionService.listCatalog());
    }
}
