package com.example.server.services;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class LicenseKeyGenerator {

    private static final String PREFIX = "LIC-";
    private static final int KEY_LENGTH = 16;
    private static final SecureRandom random = new SecureRandom();

    public String generateCode() {
        byte[] bytes = new byte[KEY_LENGTH];
        random.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return PREFIX + encoded;
    }
}