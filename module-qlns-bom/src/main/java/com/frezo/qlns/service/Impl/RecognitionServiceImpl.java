package com.frezo.qlns.service.Impl;

import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.dto.request.TokenGiftRequest;
import com.frezo.qlns.dto.request.TokenRedeemCreateRequest;
import com.frezo.qlns.dto.request.TokenRedeemReviewRequest;
import com.frezo.qlns.dto.response.RecognitionConfigResponse;
import com.frezo.qlns.dto.response.TokenRedeemResponse;
import com.frezo.qlns.dto.response.TokenRewardCatalogResponse;
import com.frezo.qlns.dto.response.TokenTransferResponse;
import com.frezo.qlns.dto.response.TokenWalletResponse;
import com.frezo.qlns.entity.TokenRedeemRequest;
import com.frezo.qlns.entity.TokenRewardCatalog;
import com.frezo.qlns.entity.TokenTransfer;
import com.frezo.qlns.entity.TokenWallet;
import com.frezo.qlns.recognition.RecognitionConfig;
import com.frezo.qlns.repository.TokenRedeemRequestRepository;
import com.frezo.qlns.repository.TokenRewardCatalogRepository;
import com.frezo.qlns.repository.TokenTransferRepository;
import com.frezo.qlns.repository.TokenWalletRepository;
import com.frezo.qlns.service.RecognitionService;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecognitionServiceImpl implements RecognitionService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TokenWalletRepository walletRepository;
    private final TokenTransferRepository transferRepository;
    private final TokenRedeemRequestRepository redeemRepository;
    private final TokenRewardCatalogRepository catalogRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    @Override
    public RecognitionConfigResponse getConfig() {
        return RecognitionConfigResponse.builder()
                .tokenToVnd(RecognitionConfig.TOKEN_TO_VND)
                .maxGiftAmount(RecognitionConfig.MAX_GIFT_AMOUNT)
                .maxRedeemAmount(RecognitionConfig.MAX_REDEEM_AMOUNT)
                .starterBalance(RecognitionConfig.STARTER_BALANCE)
                .build();
    }

    @Override
    @Transactional
    public TokenWalletResponse getMyWallet() {
        return toWalletDto(ensureWallet(requireCurrentPersonId()));
    }

    @Override
    @Transactional
    public TokenWalletResponse getWallet(String personId) {
        assertPersonExists(personId);
        return toWalletDto(ensureWallet(personId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenWalletResponse> listWallets() {
        Map<String, String> names = personNameMap();
        return walletRepository.findByIsDeletedFalseOrderByUpdatedDateDesc().stream()
                .map(w -> toWalletDto(w, names.get(w.getPersonId())))
                .toList();
    }

    @Override
    @Transactional
    public TokenTransferResponse gift(TokenGiftRequest request) {
        String fromPersonId = requireCurrentPersonId();
        String toPersonId = request.getToPersonId();
        if (toPersonId == null || toPersonId.isBlank()) {
            throw new AppException(QlnsErrorCode.TOKEN_PERSON_REQUIRED);
        }
        if (fromPersonId.equals(toPersonId)) {
            throw new AppException(QlnsErrorCode.TOKEN_SELF_GIFT);
        }
        assertPersonExists(toPersonId);

        BigDecimal amount = normalizeAmount(request.getAmount());
        if (amount.compareTo(BigDecimal.valueOf(RecognitionConfig.MAX_GIFT_AMOUNT)) > 0) {
            throw new AppException(QlnsErrorCode.TOKEN_GIFT_MAX_EXCEEDED, RecognitionConfig.MAX_GIFT_AMOUNT);
        }

        TokenWallet from = lockWallet(fromPersonId);
        TokenWallet to = lockWallet(toPersonId);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new AppException(QlnsErrorCode.TOKEN_INSUFFICIENT_BALANCE);
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        walletRepository.save(from);
        walletRepository.save(to);

        String sourceType = request.getSourceType() == null || request.getSourceType().isBlank()
                ? RecognitionConfig.SOURCE_MANUAL
                : request.getSourceType().trim().toUpperCase();
        if (!RecognitionConfig.SOURCE_TASK.equals(sourceType)) {
            sourceType = RecognitionConfig.SOURCE_MANUAL;
        }

        TokenTransfer xfer = TokenTransfer.builder()
                .fromPersonId(fromPersonId)
                .toPersonId(toPersonId)
                .amount(amount)
                .note(request.getNote())
                .sourceType(sourceType)
                .sourceId(blankToNull(request.getSourceId()))
                .build();
        xfer.setId(UUID.randomUUID().toString());
        return toTransferDto(transferRepository.save(xfer), personNameMap());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenTransferResponse> listTransfers(String personId) {
        Map<String, String> names = personNameMap();
        return transferRepository.findHistory(blankToNull(personId)).stream()
                .map(t -> toTransferDto(t, names))
                .toList();
    }

    @Override
    @Transactional
    public TokenRedeemResponse requestRedeem(TokenRedeemCreateRequest request) {
        String personId = requireCurrentPersonId();
        BigDecimal amount = normalizeAmount(request.getAmount());
        if (amount.compareTo(BigDecimal.valueOf(RecognitionConfig.MAX_REDEEM_AMOUNT)) > 0) {
            throw new AppException(QlnsErrorCode.TOKEN_INVALID_AMOUNT);
        }

        TokenWallet wallet = lockWallet(personId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException(QlnsErrorCode.TOKEN_INSUFFICIENT_BALANCE);
        }

        // Hold tokens ngay khi tạo yêu cầu — tránh double-spend; reject sẽ hoàn.
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        BigDecimal cash = amount.multiply(RecognitionConfig.TOKEN_TO_VND).setScale(0, RoundingMode.HALF_UP);
        TokenRedeemRequest redeem = TokenRedeemRequest.builder()
                .personId(personId)
                .amount(amount)
                .cashValue(cash)
                .note(request.getNote())
                .status(RecognitionConfig.REDEEM_PENDING)
                .build();
        redeem.setId(UUID.randomUUID().toString());
        return toRedeemDto(redeemRepository.save(redeem), personNameMap());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenRedeemResponse> listRedeems(String personId, String status) {
        Map<String, String> names = personNameMap();
        List<TokenRedeemRequest> list;
        if (status != null && !status.isBlank()) {
            list = redeemRepository.findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(status.trim().toUpperCase());
            if (personId != null && !personId.isBlank()) {
                list = list.stream().filter(r -> personId.equals(r.getPersonId())).toList();
            }
        } else if (personId != null && !personId.isBlank()) {
            list = redeemRepository.findByPersonIdAndIsDeletedFalseOrderByCreatedDateDesc(personId);
        } else {
            list = redeemRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        }
        return list.stream().map(r -> toRedeemDto(r, names)).toList();
    }

    @Override
    @Transactional
    public TokenRedeemResponse approveRedeem(String id) {
        TokenRedeemRequest redeem = findRedeem(id);
        if (!RecognitionConfig.REDEEM_PENDING.equals(redeem.getStatus())) {
            throw new AppException(QlnsErrorCode.TOKEN_REDEEM_INVALID_STATUS);
        }

        LocalDate next = LocalDate.now();
        redeem.setStatus(RecognitionConfig.REDEEM_APPROVED);
        redeem.setTargetMonth(next.getMonthValue());
        redeem.setTargetYear(next.getYear());
        redeem.setReviewedBy(SystemUtils.getCurrentUsername());
        redeem.setReviewedAt(LocalDateTime.now());
        return toRedeemDto(redeemRepository.save(redeem), personNameMap());
    }

    @Override
    @Transactional
    public TokenRedeemResponse rejectRedeem(String id, TokenRedeemReviewRequest request) {
        TokenRedeemRequest redeem = findRedeem(id);
        if (!RecognitionConfig.REDEEM_PENDING.equals(redeem.getStatus())) {
            throw new AppException(QlnsErrorCode.TOKEN_REDEEM_INVALID_STATUS);
        }

        TokenWallet wallet = lockWallet(redeem.getPersonId());
        wallet.setBalance(wallet.getBalance().add(redeem.getAmount()));
        walletRepository.save(wallet);

        redeem.setStatus(RecognitionConfig.REDEEM_REJECTED);
        redeem.setReviewedBy(SystemUtils.getCurrentUsername());
        redeem.setReviewedAt(LocalDateTime.now());
        redeem.setRejectReason(request != null ? request.getReason() : null);
        return toRedeemDto(redeemRepository.save(redeem), personNameMap());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenRewardCatalogResponse> listCatalog() {
        return catalogRepository.findByActiveTrueAndIsDeletedFalseOrderByTokenCostAsc().stream()
                .map(this::toCatalogDto)
                .toList();
    }

    @Override
    @Transactional
    public BigDecimal consumeApprovedForPayroll(String personId, Integer month, Integer year) {
        List<TokenRedeemRequest> list = redeemRepository.findApprovedForPayroll(
                personId, month, year, RecognitionConfig.REDEEM_APPROVED);
        if (list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (TokenRedeemRequest r : list) {
            total = total.add(r.getCashValue() != null ? r.getCashValue() : BigDecimal.ZERO);
            r.setStatus(RecognitionConfig.REDEEM_PAID);
            redeemRepository.save(r);
        }
        return total;
    }

    // ---- helpers ----

    private TokenRedeemRequest findRedeem(String id) {
        return redeemRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new AppException(QlnsErrorCode.TOKEN_REDEEM_NOT_FOUND));
    }

    private String requireCurrentPersonId() {
        String username = SystemUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new AppException(QlnsErrorCode.PERSON_NOT_FOUND);
        }
        return userRepository.findByUserName(username)
                .map(u -> u.getPersonId())
                .filter(id -> id != null && !id.isBlank())
                .orElseThrow(() -> new AppException(QlnsErrorCode.PERSON_NOT_FOUND));
    }

    private void assertPersonExists(String personId) {
        if (!personRepository.existsById(personId)) {
            throw new AppException(QlnsErrorCode.PERSON_NOT_FOUND);
        }
    }

    private TokenWallet ensureWallet(String personId) {
        return walletRepository.findByPersonIdAndIsDeletedFalse(personId)
                .orElseGet(() -> {
                    TokenWallet w = TokenWallet.builder()
                            .personId(personId)
                            .balance(RecognitionConfig.STARTER_BALANCE)
                            .build();
                    w.setId(UUID.randomUUID().toString());
                    return walletRepository.save(w);
                });
    }

    private TokenWallet lockWallet(String personId) {
        return walletRepository.findForUpdate(personId)
                .orElseGet(() -> {
                    ensureWallet(personId);
                    return walletRepository.findForUpdate(personId)
                            .orElseThrow(() -> new AppException(QlnsErrorCode.PERSON_NOT_FOUND));
                });
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(QlnsErrorCode.TOKEN_INVALID_AMOUNT);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, String> personNameMap() {
        Map<String, String> map = new HashMap<>();
        for (Person p : personRepository.findAll()) {
            if (p.getId() != null) {
                map.put(p.getId(), p.getName());
            }
        }
        return map;
    }

    private TokenWalletResponse toWalletDto(TokenWallet w) {
        String name = personRepository.findById(w.getPersonId()).map(Person::getName).orElse(null);
        return toWalletDto(w, name);
    }

    private TokenWalletResponse toWalletDto(TokenWallet w, String personName) {
        BigDecimal bal = w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO;
        return TokenWalletResponse.builder()
                .id(w.getId())
                .personId(w.getPersonId())
                .personName(personName)
                .balance(bal)
                .estimatedVnd(bal.multiply(RecognitionConfig.TOKEN_TO_VND).setScale(0, RoundingMode.HALF_UP))
                .tokenToVnd(RecognitionConfig.TOKEN_TO_VND)
                .build();
    }

    private TokenTransferResponse toTransferDto(TokenTransfer t, Map<String, String> names) {
        return TokenTransferResponse.builder()
                .id(t.getId())
                .fromPersonId(t.getFromPersonId())
                .fromPersonName(names.get(t.getFromPersonId()))
                .toPersonId(t.getToPersonId())
                .toPersonName(names.get(t.getToPersonId()))
                .amount(t.getAmount())
                .note(t.getNote())
                .sourceType(t.getSourceType())
                .sourceId(t.getSourceId())
                .createdDate(t.getCreatedDate() != null ? t.getCreatedDate().format(ISO) : null)
                .createdBy(t.getCreatedBy())
                .build();
    }

    private TokenRedeemResponse toRedeemDto(TokenRedeemRequest r, Map<String, String> names) {
        return TokenRedeemResponse.builder()
                .id(r.getId())
                .personId(r.getPersonId())
                .personName(names.get(r.getPersonId()))
                .amount(r.getAmount())
                .cashValue(r.getCashValue())
                .note(r.getNote())
                .status(r.getStatus())
                .payrollPeriodId(r.getPayrollPeriodId())
                .targetMonth(r.getTargetMonth())
                .targetYear(r.getTargetYear())
                .reviewedBy(r.getReviewedBy())
                .reviewedAt(r.getReviewedAt() != null ? r.getReviewedAt().format(ISO) : null)
                .rejectReason(r.getRejectReason())
                .createdDate(r.getCreatedDate() != null ? r.getCreatedDate().format(ISO) : null)
                .build();
    }

    private TokenRewardCatalogResponse toCatalogDto(TokenRewardCatalog c) {
        return TokenRewardCatalogResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .tokenCost(c.getTokenCost())
                .cashValue(c.getCashValue())
                .description(c.getDescription())
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
