package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountingErrorCode;
import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.dto.request.AccountRequest;
import com.frezo.accounting.dto.response.AccountResponse;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.seed.CoaSeedItem;
import com.frezo.accounting.seed.CoaTT133;
import com.frezo.accounting.seed.CoaTT99;
import com.frezo.accounting.service.AccountService;
import com.frezo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repo;

    @Override
    @Transactional
    public AccountResponse create(AccountRequest req) {
        if (repo.existsByCode(req.getCode())) {
            throw new AppException(AccountingErrorCode.ACCOUNT_CODE_EXISTS, req.getCode());
        }
        Account e = toEntity(req);
        return toResponse(repo.save(e));
    }

    @Override
    @Transactional
    public AccountResponse update(String id, AccountRequest req) {
        Account e = repo.findById(id)
                .orElseThrow(() -> new AppException(AccountingErrorCode.ACCOUNT_NOT_FOUND, id));
        // Không cho đổi code nếu đã có TK trùng
        if (!e.getCode().equals(req.getCode()) && repo.existsByCode(req.getCode())) {
            throw new AppException(AccountingErrorCode.ACCOUNT_CODE_EXISTS, req.getCode());
        }
        e.setCode(req.getCode());
        e.setName(req.getName());
        e.setType(req.getType());
        e.setStandard(req.getStandard());
        e.setLevel(req.getLevel());
        e.setParentId(req.getParentId());
        if (req.getPostable() != null) e.setPostable(req.getPostable());
        if (req.getRequiresPartner() != null) e.setRequiresPartner(req.getRequiresPartner());
        if (req.getOpeningBalance() != null) e.setOpeningBalance(req.getOpeningBalance());
        if (req.getActive() != null) e.setActive(req.getActive());
        e.setDescription(req.getDescription());
        return toResponse(repo.save(e));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Account e = repo.findById(id)
                .orElseThrow(() -> new AppException(AccountingErrorCode.ACCOUNT_NOT_FOUND, id));
        e.setIsDeleted(true);
        e.setActive(false);
        repo.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getById(String id) {
        return repo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AppException(AccountingErrorCode.ACCOUNT_NOT_FOUND, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findEntityByCode(String code) {
        return repo.findByCodeAndIsDeletedFalse(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listAll() {
        return repo.findByIsDeletedFalseOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listByStandard(AccountingStandard standard) {
        return repo.findByStandardAndIsDeletedFalseOrderByCodeAsc(standard).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int seedChartOfAccounts(AccountingStandard standard) {
        List<CoaSeedItem> items = standard == AccountingStandard.TT99
                ? CoaTT99.ITEMS
                : CoaTT133.ITEMS;

        Map<String, String> codeToId = new HashMap<>();
        // Nạp code -> id của TK đã tồn tại cùng standard
        for (Account existing : repo.findByStandardAndIsDeletedFalseOrderByCodeAsc(standard)) {
            codeToId.put(existing.getCode(), existing.getId());
        }

        int created = 0;
        for (CoaSeedItem item : items) {
            if (codeToId.containsKey(item.getCode())) continue;
            String parentId = item.getParentCode() != null
                    ? codeToId.get(item.getParentCode())
                    : null;
            Account e = Account.builder()
                    .code(item.getCode())
                    .name(item.getName())
                    .type(item.getType())
                    .standard(standard)
                    .level(item.getLevel())
                    .parentId(parentId)
                    .postable(item.isPostable())
                    .requiresPartner(item.isRequiresPartner())
                    .openingBalance(BigDecimal.ZERO)
                    .active(true)
                    .build();
            e.setId(UUID.randomUUID().toString());
            e.setIsDeleted(false);
            Account saved = repo.save(e);
            codeToId.put(saved.getCode(), saved.getId());
            created++;
        }
        return created;
    }

    private Account toEntity(AccountRequest r) {
        return Account.builder()
                .code(r.getCode())
                .name(r.getName())
                .type(r.getType())
                .standard(r.getStandard())
                .level(r.getLevel())
                .parentId(r.getParentId())
                .postable(r.getPostable() != null ? r.getPostable() : true)
                .requiresPartner(r.getRequiresPartner() != null ? r.getRequiresPartner() : false)
                .openingBalance(r.getOpeningBalance() != null ? r.getOpeningBalance() : BigDecimal.ZERO)
                .active(r.getActive() != null ? r.getActive() : true)
                .description(r.getDescription())
                .build();
    }

    private AccountResponse toResponse(Account e) {
        return AccountResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .type(e.getType())
                .standard(e.getStandard())
                .level(e.getLevel())
                .parentId(e.getParentId())
                .postable(e.getPostable())
                .requiresPartner(e.getRequiresPartner())
                .openingBalance(e.getOpeningBalance())
                .active(e.getActive())
                .description(e.getDescription())
                .build();
    }
}
