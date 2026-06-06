package com.frezo.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class for password hashing and verification
 * Used for testing and migrating plain text passwords to BCrypt
 */
public class PasswordHashUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Hash a plain text password using BCrypt
     * 
     * @param plainPassword Plain text password
     * @return BCrypt hashed password
     */
    public static String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    /**
     * Verify if a plain text password matches a BCrypt hash
     * 
     * @param plainPassword  Plain text password
     * @param hashedPassword BCrypt hashed password
     * @return true if matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Main method for testing password hashing
     * Usage: Run this class and enter passwords to get their BCrypt hashes
     */
    public static void main(String[] args) {
        // Example passwords to hash
        String[] testPasswords = {
                "admin123",
                "user123",
                "password",
                "123456"
        };

        System.out.println("=== BCrypt Password Hash Generator ===\n");

        for (String password : testPasswords) {
            String hashed = hashPassword(password);
            System.out.println("Plain text: " + password);
            System.out.println("BCrypt hash: " + hashed);
            System.out.println("Verification: " + verifyPassword(password, hashed));
            System.out.println("---");
        }

        // Generate SQL UPDATE statements
        System.out.println("\n=== SQL UPDATE Statements ===\n");
        System.out.println("-- Update existing users with hashed passwords");
        System.out.println("-- Replace 'your_username' and 'plain_password' with actual values\n");

        for (String password : testPasswords) {
            String hashed = hashPassword(password);
            System.out.println("-- For password: " + password);
            System.out.println("UPDATE users SET password = '" + hashed + "' WHERE user_name = 'your_username';");
            System.out.println();
        }
    }
}
