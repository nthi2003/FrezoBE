package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.TokenGiftRequest;
import com.frezo.qlns.dto.request.TokenRedeemCreateRequest;
import com.frezo.qlns.dto.request.TokenRedeemReviewRequest;
import com.frezo.qlns.dto.response.RecognitionConfigResponse;
import com.frezo.qlns.dto.response.TokenRedeemResponse;
import com.frezo.qlns.dto.response.TokenRewardCatalogResponse;
import com.frezo.qlns.dto.response.TokenTransferResponse;
import com.frezo.qlns.dto.response.TokenWalletResponse;

import java.math.BigDecimal;
import java.util.List;

public interface RecognitionService {

    RecognitionConfigResponse getConfig();

    TokenWalletResponse getMyWallet();

    TokenWalletResponse getWallet(String personId);

    List<TokenWalletResponse> listWallets();

    TokenTransferResponse gift(TokenGiftRequest request);

    List<TokenTransferResponse> listTransfers(String personId);

    TokenRedeemResponse requestRedeem(TokenRedeemCreateRequest request);

    List<TokenRedeemResponse> listRedeems(String personId, String status);

    TokenRedeemResponse approveRedeem(String id);

    TokenRedeemResponse rejectRedeem(String id, TokenRedeemReviewRequest request);

    List<TokenRewardCatalogResponse> listCatalog();

    /** Cộng dồn cashValue yêu cầu APPROVED vào kỳ lương; đánh dấu PAID. */
    BigDecimal consumeApprovedForPayroll(String personId, Integer month, Integer year);
}
