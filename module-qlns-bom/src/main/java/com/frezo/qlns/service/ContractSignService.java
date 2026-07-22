package com.frezo.qlns.service;

import java.util.Map;

public interface ContractSignService {
    Map<String, Object> requestOtp(String contractId, String ip, String device);
    Map<String, Object> confirm(String contractId, String otp, String ip, String device);
    /** Trạng thái ký hiện tại — FE poll / load page. */
    Map<String, Object> status(String contractId);
}
