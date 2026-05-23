package com.example.server.signature;

import com.example.server.config.SignatureProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

@Slf4j
@Service
public class SigningService {

    private final Canonicalizer canonicalizer;
    private final KeyProvider keyProvider;
    private final SignatureProperties properties;

    public SigningService(Canonicalizer canonicalizer,
                          KeyProvider keyProvider,
                          SignatureProperties properties) {
        this.canonicalizer = canonicalizer;
        this.keyProvider = keyProvider;
        this.properties = properties;
    }

    public String sign(Object payload) {
        try {
            byte[] canonicalBytes;
            try {
                canonicalBytes = canonicalizer.canonicalize(payload);
                log.debug("Canonicalized payload to {} bytes", canonicalBytes.length);
            } catch (SignatureException e) {
                throw e;
            } catch (Exception e) {
                throw new SignatureException(
                        SignatureException.ErrorCode.CANONICALIZATION_ERROR,
                        "Canonicalization failed: " + e.getMessage(),
                        e
                );
            }

            return signBytesToString(canonicalBytes);
        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException(
                    SignatureException.ErrorCode.SIGN_OPERATION_FAILED,
                    "Unexpected error during signing: " + e.getMessage(),
                    e
            );
        }
    }

    public byte[] signBytes(byte[] data) throws Exception {
        PrivateKey privateKey;
        try {
            privateKey = keyProvider.getSigningKey();
        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException(
                    SignatureException.ErrorCode.KEY_PROVIDER_ERROR,
                    "Failed to get signing key: " + e.getMessage(),
                    e
            );
        }

        try {
            Signature signature = Signature.getInstance(properties.getSignatureAlgorithm());
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new SignatureException(
                    SignatureException.ErrorCode.SIGN_OPERATION_FAILED,
                    "Sign operation failed: " + e.getMessage(),
                    e
            );
        }
    }

    public String signBytesToString(byte[] data) {
        try {
            byte[] signatureBytes = signBytes(data);
            log.debug("Generated signature of {} bytes", signatureBytes.length);
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException(
                    SignatureException.ErrorCode.SIGN_OPERATION_FAILED,
                    "Sign operation failed: " + e.getMessage(),
                    e
            );
        }
    }

    public boolean verify(String signatureBase64, Object payload) throws Exception {
        byte[] canonicalBytes = canonicalizer.canonicalize(payload);
        java.security.PublicKey publicKey = keyProvider.getCertificate().getPublicKey();
        Signature verifier = Signature.getInstance(properties.getSignatureAlgorithm());
        verifier.initVerify(publicKey);
        verifier.update(canonicalBytes);
        return verifier.verify(Base64.getDecoder().decode(signatureBase64));
    }
}