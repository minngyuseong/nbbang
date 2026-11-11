package com.nbbang.config;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final String databasePassword;
    private final String accountEncryptionKey;
    private final String webOrigin;
    private final String kakaoClientId;
    private final String kakaoClientSecret;
    private final String googleClientId;
    private final String googleClientSecret;
    private final String vapidPublicKey;
    private final String vapidPrivateKey;

    public ProductionConfigurationValidator(
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.account-encryption-key}") String accountEncryptionKey,
            @Value("${app.web-origin}") String webOrigin,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}") String kakaoClientId,
            @Value("${spring.security.oauth2.client.registration.kakao.client-secret}") String kakaoClientSecret,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String googleClientSecret,
            @Value("${app.push.public-key}") String vapidPublicKey,
            @Value("${app.push.private-key}") String vapidPrivateKey) {
        this.databasePassword = databasePassword;
        this.accountEncryptionKey = accountEncryptionKey;
        this.webOrigin = webOrigin;
        this.kakaoClientId = kakaoClientId;
        this.kakaoClientSecret = kakaoClientSecret;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.vapidPublicKey = vapidPublicKey;
        this.vapidPrivateKey = vapidPrivateKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireSecret("DB_PASSWORD", databasePassword, 16);
        requireSecret("KAKAO_CLIENT_ID", kakaoClientId, 4);
        requireSecret("KAKAO_CLIENT_SECRET", kakaoClientSecret, 8);
        requireSecret("GOOGLE_CLIENT_ID", googleClientId, 8);
        requireSecret("GOOGLE_CLIENT_SECRET", googleClientSecret, 8);
        requireSecret("VAPID_PUBLIC_KEY", vapidPublicKey, 16);
        requireSecret("VAPID_PRIVATE_KEY", vapidPrivateKey, 16);
        if (!webOrigin.startsWith("https://"))
            throw new IllegalStateException("WEB_ORIGIN must use HTTPS in production.");
        try {
            if (Base64.getDecoder().decode(accountEncryptionKey).length != 32)
                throw new IllegalStateException("ACCOUNT_ENCRYPTION_KEY must decode to exactly 32 bytes.");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ACCOUNT_ENCRYPTION_KEY must be valid Base64.", exception);
        }
    }

    private void requireSecret(String name, String value, int minimumLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < minimumLength || normalized.toLowerCase().contains("change-me")
                || normalized.toLowerCase().contains("placeholder"))
            throw new IllegalStateException(name + " is missing or unsafe for production.");
    }
}
