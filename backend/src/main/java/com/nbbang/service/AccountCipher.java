package com.nbbang.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountCipher {
    private static final int IV_SIZE = 12;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AccountCipher(@Value("${app.account-encryption-key}") String encodedKey) {
        byte[] bytes = Base64.getDecoder().decode(encodedKey);
        if (bytes.length != 32) throw new IllegalArgumentException("계좌 암호화 키는 Base64로 인코딩한 32바이트여야 합니다.");
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_SIZE]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) { throw new IllegalStateException("계좌정보 암호화에 실패했습니다.", e); }
    }

    public String decrypt(String encoded) {
        try {
            byte[] payload = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_SIZE]; buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()]; buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("계좌정보 복호화에 실패했습니다.", e);
        }
    }
}
