package com.nbbang.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter @Entity @Table(name = "push_subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, unique = true, columnDefinition = "text") private String endpoint;
    @Column(nullable = false, columnDefinition = "text") private String p256dh;
    @Column(nullable = false, columnDefinition = "text") private String auth;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user = user; this.endpoint = endpoint; this.p256dh = p256dh; this.auth = auth;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void refresh(User user, String p256dh, String auth) {
        this.user = user; this.p256dh = p256dh; this.auth = auth; this.updatedAt = Instant.now();
    }
}
