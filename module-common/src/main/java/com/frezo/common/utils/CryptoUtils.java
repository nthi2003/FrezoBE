package com.frezo.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    // Ideally, this key should be loaded from an environment variable or secret manager
    // For demonstration, a static key is used. 
    // In production: System.getenv("AES_SECRET_KEY")
    private static final String SECRET_KEY_STRING = "12345678901234567890123456789012"; // 32 bytes for AES-256

    public static byte[] encryptAESGCM(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKey secretKey = new SecretKeySpec(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8), "AES");
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            byte[] cipherTextWithIv = new byte[IV_LENGTH_BYTE + cipherText.length];
            System.arraycopy(iv, 0, cipherTextWithIv, 0, IV_LENGTH_BYTE);
            System.arraycopy(cipherText, 0, cipherTextWithIv, IV_LENGTH_BYTE, cipherText.length);
            
            return cipherTextWithIv;
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting text", e);
        }
    }

    public static String decryptAESGCM(byte[] cipherTextWithIv) {
        if (cipherTextWithIv == null) return null;
        try {
            SecretKey secretKey = new SecretKeySpec(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8), "AES");
            
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(cipherTextWithIv, 0, iv, 0, IV_LENGTH_BYTE);
            
            byte[] cipherText = new byte[cipherTextWithIv.length - IV_LENGTH_BYTE];
            System.arraycopy(cipherTextWithIv, IV_LENGTH_BYTE, cipherText, 0, cipherText.length);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting text", e);
        }
    }

    public static String hashSHA256(String text) {
        if (text == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing text", e);
        }
    }

    public static String getLast4Digits(String phone) {
        if (phone == null) return null;
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() <= 4) return cleanPhone;
        return cleanPhone.substring(cleanPhone.length() - 4);
    }
}
