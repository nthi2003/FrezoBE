package com.frezo.auth.util;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;


public class PasswordHashUtil {

    private static final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();


    public static String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }


    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }

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
