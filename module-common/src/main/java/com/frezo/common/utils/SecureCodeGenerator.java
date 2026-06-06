package com.frezo.common.utils;

import java.security.SecureRandom;

public class SecureCodeGenerator {
    
    // Bảng chữ cái loại bỏ các ký tự dễ nhầm lẫn như I, O, 0, 1, L
    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Tạo mã code ngẫu nhiên (Ví dụ: NCC-7A9B3F)
     * Thích hợp bảo mật STT, định danh để không lộ số lượng record trong CSDL, chống crawl.
     * 
     * @param prefix Tiền tố mã (Ví dụ: "NCC", "KH")
     * @return Chuỗi mã được tạo
     */
    public static String generateCode(String prefix) {
        return generateCode(prefix, DEFAULT_CODE_LENGTH);
    }

    /**
     * Tạo mã code ngẫu nhiên với độ dài tùy chỉnh
     * 
     * @param prefix Tiền tố mã (Ví dụ: "NCC", "KH")
     * @param length Độ dài chuỗi ngẫu nhiên
     * @return Chuỗi mã được tạo
     */
    public static String generateCode(String prefix, int length) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.trim().isEmpty()) {
            sb.append(prefix.trim().toUpperCase()).append("-");
        }
        
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
