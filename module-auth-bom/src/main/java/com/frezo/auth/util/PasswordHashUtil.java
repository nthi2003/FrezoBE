package com.frezo.auth.util;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class for password hashing and verification.
 * Dùng {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()} để output
 * luôn có prefix {@code {bcrypt}} — khớp SecurityConfig DelegatingPasswordEncoder.
 * <p>
 * <b>CẤM</b> trả về raw {@code $2a$...} không prefix — login sẽ lỗi
 * {@code There is no PasswordEncoder mapped for the id "null"}.
 */
public class PasswordHashUtil {

    private static final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    /**
     * Hash plain text → {@code {bcrypt}$2a$10$...}
     */
    public static String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    /**
     * Verify plain text against stored hash (phải có prefix {@code {id}}).
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }

    /**
     * CLI: sinh hash + SQL UPDATE cho seed/migration.
     */
    public static void main(String[] args) {
        String[] testPasswords = {
                "admin123",
                "user123",
                "password",
                "123456"
        };

        System.out.println("=== DelegatingPasswordEncoder ({bcrypt}) Hash Generator ===\n");
        System.out.println("-- Prerequisite: ALTER TABLE users ALTER COLUMN password TYPE varchar(255);\n");

        for (String password : testPasswords) {
            String hashed = hashPassword(password);
            System.out.println("Plain text: " + password);
            System.out.println("Encoded:    " + hashed);
            System.out.println("Verify:     " + verifyPassword(password, hashed));
            System.out.println("---");
        }

        System.out.println("\n=== SQL UPDATE Statements ===\n");
        for (String password : testPasswords) {
            String hashed = hashPassword(password);
            System.out.println("-- password: " + password);
            System.out.println("UPDATE users SET password = '" + hashed + "' WHERE user_name = 'your_username';");
            System.out.println();
        }
    }
}
