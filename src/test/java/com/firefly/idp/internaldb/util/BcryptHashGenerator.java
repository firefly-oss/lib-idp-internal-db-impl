package com.firefly.idp.internaldb.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility test to generate BCrypt hashes.
 * Run this test and check the output to get the correct hash.
 */
public class BcryptHashGenerator {

    @Test
    void generateAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("=========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash (strength 12): " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
        System.out.println("=========================================");
    }
}
