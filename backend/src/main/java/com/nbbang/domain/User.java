package com.nbbang.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id @UuidGenerator private UUID id;
    @Column(nullable = false, length = 20) private String provider;
    @Column(name = "provider_user_id", nullable = false) private String providerUserId;
    private String email;
    @Column(name = "display_name", nullable = false, length = 100) private String displayName;
    @Column(name = "profile_image_url") private String profileImageUrl;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public User(String provider, String providerUserId, String email, String displayName, String profileImageUrl) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateProfile(String email, String displayName, String profileImageUrl) {
        this.email = email;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = Instant.now();
    }

    public void anonymize() {
        this.provider = "deleted";
        this.providerUserId = UUID.randomUUID().toString();
        this.email = null;
        this.displayName = "탈퇴한 사용자";
        this.profileImageUrl = null;
        this.updatedAt = Instant.now();
    }
}

