package com.example.server.binary;

import com.example.server.signature.SigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinarySignatureService {

    private final SigningService signingService;

    public byte[] signBinary(byte[] data) {
        try {
            // Вычисляем хеш данных
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            // Подписываем хеш
            String signature = signingService.sign(hash);

            log.info("Binary signature generated, data size: {} bytes", data.length);
            return Base64.getDecoder().decode(signature);

        } catch (Exception e) {
            log.error("Failed to sign binary data: {}", e.getMessage());
            throw new RuntimeException("Binary signing failed", e);
        }
    }

    public boolean verifyBinarySignature(byte[] data, byte[] signature) {
        try {
            byte[] expectedSignature = signBinary(data);
            return MessageDigest.isEqual(expectedSignature, signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}