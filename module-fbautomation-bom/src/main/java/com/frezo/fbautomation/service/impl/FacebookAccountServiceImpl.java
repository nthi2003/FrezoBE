package com.frezo.fbautomation.service.impl;

import com.frezo.common.exception.QTHTException;
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

    @Override
    public List<FacebookAccountResponse> getAll() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    public FacebookAccountResponse getById(String id) {
        return accountMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public FacebookAccountResponse create(FacebookAccountRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new QTHTException("Tài khoản " + request.getUsername() + " đã tồn tại");
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

    @Override
    @Transactional
    public FacebookAccountResponse update(String id, FacebookAccountRequest request) {
        FacebookAccount entity = findById(id);
        accountMapper.updateEntity(entity, request);
        return accountMapper.toResponse(accountRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        findById(id);
        accountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateCookie(String id, String cookie) {
        FacebookAccount entity = findById(id);
        entity.setCookie(cookie);
        accountRepository.save(entity);
    }

    @Override
    @Transactional
    public void incrementPostsToday(String id) {
        FacebookAccount entity = findById(id);
        entity.setPostsToday(entity.getPostsToday() == null ? 1 : entity.getPostsToday() + 1);
        accountRepository.save(entity);
    }

    @Override
    @Transactional
    public void resetDailyPostCount() {
        List<FacebookAccount> accounts = accountRepository.findByStatus("ACTIVE");
        accounts.forEach(a -> a.setPostsToday(0));
        accountRepository.saveAll(accounts);
        log.info("Đã reset postsToday cho {} tài khoản", accounts.size());
    }

    private FacebookAccount findById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy tài khoản Facebook"));
    }
}
