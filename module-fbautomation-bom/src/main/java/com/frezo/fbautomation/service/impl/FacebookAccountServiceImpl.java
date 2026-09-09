package com.frezo.fbautomation.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.fbautomation.common.FbAutomationErrorCode;
import com.frezo.fbautomation.dto.request.FacebookAccountRequest;
import com.frezo.fbautomation.dto.response.AutomationSummaryResponse;
import com.frezo.fbautomation.dto.response.FacebookAccountResponse;
import com.frezo.fbautomation.entity.FacebookAccount;
import com.frezo.fbautomation.mapper.FacebookAccountMapper;
import com.frezo.fbautomation.repository.FacebookAccountRepository;
import com.frezo.fbautomation.service.FacebookAccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookAccountServiceImpl implements FacebookAccountService {

    private final FacebookAccountRepository accountRepository;
    private final FacebookAccountMapper accountMapper;

    /** Lấy danh sách tất cả tài khoản Facebook đã lưu. */
    @Override
    public List<FacebookAccountResponse> getAll() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    /** Lấy thông tin chi tiết tài khoản Facebook theo mã định danh. */
    @Override
    public FacebookAccountResponse getById(String id) {
        return accountMapper.toResponse(findById(id));
    }

    /** Tạo mới tài khoản Facebook, kiểm tra trùng username và gán giá trị mặc định cho trạng thái/số bài đăng. */
    @Override
    @Transactional
    public FacebookAccountResponse create(FacebookAccountRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(FbAutomationErrorCode.ACCOUNT_EXISTS, request.getUsername());
        }
        FacebookAccount entity = accountMapper.toEntity(request);
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        if (entity.getPostsToday() == null) {
            entity.setPostsToday(0);
        }
        return accountMapper.toResponse(accountRepository.save(entity));
    }

    /** Cập nhật thông tin tài khoản Facebook theo mã định danh. */
    @Override
    @Transactional
    public FacebookAccountResponse update(String id, FacebookAccountRequest request) {
        FacebookAccount entity = findById(id);
        accountMapper.updateEntity(entity, request);
        return accountMapper.toResponse(accountRepository.save(entity));
    }

    /** Xóa tài khoản Facebook theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        findById(id);
        accountRepository.deleteById(id);
    }

    /** Cập nhật cookie đăng nhập cho tài khoản Facebook theo mã định danh. */
    @Override
    @Transactional
    public void updateCookie(String id, String cookie) {
        FacebookAccount entity = findById(id);
        entity.setCookie(cookie);
        accountRepository.save(entity);
    }

    /** Tăng bộ đếm số bài đăng trong ngày của tài khoản Facebook. */
    @Override
    @Transactional
    public void incrementPostsToday(String id) {
        FacebookAccount entity = findById(id);
        entity.setPostsToday(entity.getPostsToday() == null ? 1 : entity.getPostsToday() + 1);
        accountRepository.save(entity);
    }

    /** Đặt lại bộ đếm bài đăng trong ngày về 0 cho mọi tài khoản đang ACTIVE. */
    @Override
    @Transactional
    public void resetDailyPostCount() {
        List<FacebookAccount> accounts = accountRepository.findByStatus("ACTIVE");
        accounts.forEach(a -> a.setPostsToday(0));
        accountRepository.saveAll(accounts);
        log.info("Đã reset postsToday cho {} tài khoản", accounts.size());
    }

    /** Tìm tài khoản Facebook theo mã, ném ngoại lệ nếu không tồn tại. */
    private FacebookAccount findById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AppException(FbAutomationErrorCode.ACCOUNT_NOT_FOUND));
    }
}
