package com.frezo.common.utils;

import java.security.SecureRandom;

public class SecureCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();


    public static String generateCode(String prefix) {
        return generateCode(prefix, DEFAULT_CODE_LENGTH);
    }


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
