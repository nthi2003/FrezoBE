package com.frezo.accounting.service;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.dto.request.AccountRequest;
import com.frezo.accounting.dto.response.AccountResponse;
import com.frezo.accounting.entity.Account;

import java.util.List;
import java.util.Optional;

/**
 * Quản lý Chart of Accounts (COA).
 */
public interface AccountService {

    AccountResponse create(AccountRequest req);

    AccountResponse update(String id, AccountRequest req);

    void delete(String id);

    AccountResponse getById(String id);

    Optional<Account> findEntityByCode(String code);

    List<AccountResponse> listAll();

    List<AccountResponse> listByStandard(AccountingStandard standard);

    /**
     * Nạp toàn bộ COA theo chuẩn (dùng khi khởi tạo dữ liệu).
     * <p>Idempotent — nếu đã có TK với cùng code, bỏ qua (không update).
     * @return số TK mới được tạo.
     */
    int seedChartOfAccounts(AccountingStandard standard);
}
