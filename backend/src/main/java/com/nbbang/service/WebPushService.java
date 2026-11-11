package com.nbbang.service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nbbang.domain.PushSubscription;
import com.nbbang.domain.User;
import com.nbbang.repository.PushSubscriptionRepository;
import com.nbbang.repository.UserRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;

@Service
public class WebPushService {
    private final PushSubscriptionRepository subscriptions;
    private final UserRepository users;
    private final String publicKey;
    private final PushService pushService;

    public WebPushService(PushSubscriptionRepository subscriptions, UserRepository users,
                          @Value("${app.push.public-key:}") String publicKey,
                          @Value("${app.push.private-key:}") String privateKey,
                          @Value("${app.push.subject}") String subject) throws Exception {
        this.subscriptions = subscriptions; this.users = users; this.publicKey = publicKey;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider());
        if (publicKey.isBlank() || privateKey.isBlank()) {
            this.pushService = null;
        } else {
            PushService service = new PushService();
            service.setPublicKey(Utils.loadPublicKey(publicKey));
            service.setPrivateKey(Utils.loadPrivateKey(privateKey));
            service.setSubject(subject);
            this.pushService = service;
        }
    }

    public String publicKey() { return publicKey; }

    @Transactional
    public void subscribe(UUID userId, String endpoint, String p256dh, String auth) {
        if (endpoint == null || !endpoint.startsWith("https://") || p256dh == null || auth == null)
            throw new IllegalArgumentException("올바르지 않은 푸시 구독 정보입니다.");
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        PushSubscription subscription = subscriptions.findByEndpoint(endpoint)
                .orElseGet(() -> new PushSubscription(user, endpoint, p256dh, auth));
        subscription.refresh(user, p256dh, auth);
        subscriptions.save(subscription);
    }

    public void send(UUID userId, String title, String body, String url) {
        if (pushService == null) return;
        String payloadJson = "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body)
                + "\",\"url\":\"" + escape(url) + "\"}";
        byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);
        for (PushSubscription subscription : subscriptions.findAllByUserId(userId)) {
            try {
                Notification notification = new Notification(subscription.getEndpoint(),
                        Utils.loadPublicKey(subscription.getP256dh()),
                        Base64.getUrlDecoder().decode(subscription.getAuth()), payload);
                pushService.send(notification);
            } catch (Exception exception) {
                // A later successful subscription refresh replaces rotated browser keys.
            }
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
