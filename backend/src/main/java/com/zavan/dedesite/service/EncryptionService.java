package com.zavan.dedesite.service;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static EncryptionService instance;

    @Value("${app.observatory.encryption-key:${OBSERVATORY_ENCRYPTION_KEY:}}")
    private String configuredKey;

    private SecretKeySpec key;

    @PostConstruct
    void initialize() {
        if (configuredKey == null || configuredKey.isBlank()) {
            return;
        }
        key = new SecretKeySpec(normalizeKey(configuredKey), "AES");
        instance = this;
    }

    public static String encryptForJpa(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        return requireInstance().encrypt(plaintext);
    }

    public static String decryptForJpa(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        if (!ciphertext.startsWith(PREFIX)) {
            return ciphertext;
        }
        return requireInstance().decrypt(ciphertext);
    }

    public int activeKeyVersion() {
        return 1;
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt Observatory field", e);
        }
    }

    private String decrypt(String ciphertext) {
        try {
            byte[] packed = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(packed);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt Observatory field", e);
        }
    }

    private static EncryptionService requireInstance() {
        if (instance == null || instance.key == null) {
            throw new IllegalStateException("OBSERVATORY_ENCRYPTION_KEY must be configured to use encrypted Observatory fields");
        }
        return instance;
    }

    private byte[] normalizeKey(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length == 16 || decoded.length == 24 || decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Treat non-base64 values as deployment secrets and derive an AES-256 key.
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize Observatory encryption key", e);
        }
    }
}
