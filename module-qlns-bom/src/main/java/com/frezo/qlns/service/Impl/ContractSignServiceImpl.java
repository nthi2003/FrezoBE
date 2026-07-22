package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.service.NotificationService;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.ContractSignSession;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.repository.ContractSignSessionRepository;
import com.frezo.qlns.service.ContractSignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractSignServiceImpl implements ContractSignService {

    private final ContractRepository contractRepository;
    private final ContractSignSessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public Map<String, Object> requestOtp(String contractId, String ip, String device) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Hợp đồng không tồn tại"));

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        ContractSignSession session = ContractSignSession.builder()
                .contractId(contract.getId())
                .otpHash(hash(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status("PENDING")
                .ip(ip)
                .device(device)
                .build();
        session.setId(UUID.randomUUID().toString());
        sessionRepository.save(session);

        String username = SystemUtils.getCurrentUsername();
        String msg = "OTP ký HĐ " + contract.getCode() + ": " + otp + " (hết hạn 10 phút)";
        try {
            notificationService.notifyUserWithEmailFallback(
                    username != null ? username : "admin",
                    "OTP ký hợp đồng",
                    msg,
                    true);
        } catch (Exception e) {
            log.warn("[ContractSign] email notify failed, OTP logged for contract {}: {}", contractId, otp);
        }
        log.info("[ContractSign] OTP issued for contract {} (dev-log: {})", contractId, otp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractId", contractId);
        result.put("sessionId", session.getId());
        result.put("expiresAt", session.getExpiresAt().toString());
        result.put("message", "OTP đã gửi (email hoặc log)");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> confirm(String contractId, String otp, String ip, String device) {
        ContractSignSession session = sessionRepository
                .findFirstByContractIdAndStatusAndIsDeletedFalseOrderByCreatedDateDesc(contractId, "PENDING")
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không có phiên ký PENDING"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "OTP đã hết hạn");
        }
        if (otp == null || !hash(otp).equalsIgnoreCase(session.getOtpHash())) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "OTP không đúng");
        }

        String signedBy = SystemUtils.getCurrentUsername();
        session.setStatus("SIGNED");
        session.setSignedAt(LocalDateTime.now());
        session.setSignedBy(signedBy);
        session.setIp(ip);
        session.setDevice(device);
        sessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractId", contractId);
        result.put("status", "SIGNED");
        result.put("signedAt", session.getSignedAt().toString());
        result.put("signedBy", signedBy);
        result.put("audit", Map.of("ip", ip != null ? ip : "", "device", device != null ? device : ""));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> status(String contractId) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Hợp đồng không tồn tại"));

        Optional<ContractSignSession> signed = sessionRepository
                .findFirstByContractIdAndStatusAndIsDeletedFalseOrderByCreatedDateDesc(contractId, "SIGNED");
        if (signed.isPresent()) {
            return toStatusMap(signed.get(), true);
        }

        Optional<ContractSignSession> latest = sessionRepository
                .findFirstByContractIdAndIsDeletedFalseOrderByCreatedDateDesc(contractId);
        if (latest.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("contractId", contractId);
            empty.put("signed", false);
            empty.put("status", "UNSIGNED");
            empty.put("signedAt", null);
            empty.put("signedBy", null);
            empty.put("sessionId", null);
            empty.put("expiresAt", null);
            empty.put("audit", Map.of("ip", "", "device", ""));
            return empty;
        }

        ContractSignSession session = latest.get();
        // PENDING quá hạn → đánh dấu EXPIRED khi FE poll
        if ("PENDING".equals(session.getStatus())
                && session.getExpiresAt() != null
                && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("EXPIRED");
            sessionRepository.save(session);
        }
        return toStatusMap(session, false);
    }

    private static Map<String, Object> toStatusMap(ContractSignSession session, boolean preferSigned) {
        boolean isSigned = "SIGNED".equals(session.getStatus()) || preferSigned;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractId", session.getContractId());
        result.put("signed", isSigned);
        result.put("status", session.getStatus());
        result.put("signedAt", session.getSignedAt() != null ? session.getSignedAt().toString() : null);
        result.put("signedBy", session.getSignedBy());
        result.put("sessionId", session.getId());
        result.put("expiresAt", session.getExpiresAt() != null ? session.getExpiresAt().toString() : null);
        result.put("audit", Map.of(
                "ip", session.getIp() != null ? session.getIp() : "",
                "device", session.getDevice() != null ? session.getDevice() : ""));
        return result;
    }

    private static String hash(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
