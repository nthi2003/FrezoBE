package com.frezo.qtht.service.impl;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.constant.BlockReason;
import com.frezo.common.constant.TimeBlock;
import com.frezo.qtht.entity.BlockIP;
import com.frezo.qtht.repository.BlockIPRepository;
import com.frezo.common.service.IpBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IpBlockServiceImpl implements IpBlockService {
    private final BlockIPRepository blockIPRepository;
    private final UserRepository userRepository;

    private static final int THRESHOLD_BLOCK_START = 6;
    private static final int THRESHOLD_LOCK_ACCOUNT = 11;
    @Override
    public void checkIpBlocked(String ipAddress, String userName) {
        if (SystemUtils.isNullOrEmpty(ipAddress) || SystemUtils.isNullOrEmpty(userName)) {
            return;
        }

        List<BlockIP> activeBlocks = blockIPRepository
                .findByIpAddressAndTargetUserNameAndBlockedUntilAfter(
                        ipAddress,
                        userName,
                        LocalDateTime.now());

        if (!activeBlocks.isEmpty()) {
            throw new QTHTException("block.ip");
        }
    }

    @Override
    @Transactional
    public void handleFailedAttempt(String ipAddress, String targetUserName, BlockReason reason) { // Xóa port param
        if (SystemUtils.isNullOrEmpty(ipAddress) || SystemUtils.isNullOrEmpty(targetUserName)) {
            return;
        }
        BlockIP blockIP = blockIPRepository.findByIpAddressAndTargetUserName(ipAddress, targetUserName)
                .orElse(new BlockIP());

        int oldIpAttempts = Optional.ofNullable(blockIP.getFailedAttempts()).orElse(0);
        int currentIpAttempts;

        if (blockIP.getId() != null) {
            if (blockIP.getUpdatedDate().plusDays(1).isBefore(LocalDateTime.now())) {
                currentIpAttempts = 1;
                oldIpAttempts = 0;
            } else {
                currentIpAttempts = oldIpAttempts + 1;
            }
        } else {
            blockIP.setIpAddress(ipAddress);
            blockIP.setTargetUserName(targetUserName);
            blockIP.setBlockedUntil(LocalDateTime.now());
            currentIpAttempts = 1;
        }

        blockIP.setReason(reason);
        blockIP.setFailedAttempts(currentIpAttempts);

        Integer dbTotalAttempts = blockIPRepository.sumFailedAttemptsByTargetUserName(targetUserName,
                LocalDateTime.now().minusDays(1));

        int totalForUser = Optional.ofNullable(dbTotalAttempts).orElse(0) - oldIpAttempts + currentIpAttempts;

        if (totalForUser >= THRESHOLD_LOCK_ACCOUNT) {
            lockUserAccount(targetUserName, totalForUser);
            blockIP.setBlockLevel(TimeBlock.LEVEL_MAX);
            blockIP.setBlockedUntil(LocalDateTime.now().plusYears(10));
        } else if (currentIpAttempts >= THRESHOLD_BLOCK_START) {
            int levelIndex = currentIpAttempts - THRESHOLD_BLOCK_START;
            TimeBlock nextLevel = getLevelByIndex(levelIndex);
            blockIP.setBlockLevel(nextLevel);
            blockIP.setBlockedUntil(LocalDateTime.now().plusMinutes(nextLevel.getDurationMinutes()));
        } else {
            blockIP.setBlockLevel(null);
            blockIP.setBlockedUntil(LocalDateTime.now());
        }

        blockIPRepository.save(blockIP);
    }

    @Override
    @Transactional
    public void clearFailedAttempts(String ipAddress, String userName) {
        if (SystemUtils.isNullOrEmpty(userName)) {
            return;
        }
        blockIPRepository.deleteByIpAddressAndTargetUserName(ipAddress, userName);
    }

    private void lockUserAccount(String userName, int attempts) {
        Optional<User> userOpt = userRepository.findByUserName(userName);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(0);
            user.setUpdatedDate(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    private TimeBlock getLevelByIndex(int index) {
        switch (index) {
            case 0:
                return TimeBlock.LEVEL_1;
            case 1:
                return TimeBlock.LEVEL_2;
            case 2:
                return TimeBlock.LEVEL_3;
            case 3:
                return TimeBlock.LEVEL_4;
            default:
                return TimeBlock.LEVEL_MAX;
        }
    }
}
