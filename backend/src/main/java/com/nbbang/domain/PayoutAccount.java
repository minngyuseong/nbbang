package com.nbbang.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "payout_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayoutAccount {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "institution_code", nullable = false, length = 20) private String institutionCode;
    @Column(name = "institution_name", nullable = false, length = 100) private String institutionName;
    @Column(name = "account_number_encrypted", nullable = false) private String accountNumberEncrypted;
    @Column(name = "account_number_masked", nullable = false, length = 100) private String accountNumberMasked;
    @Column(name = "account_holder", nullable = false, length = 100) private String accountHolder;
    @Column(length = 100) private String alias;
    @Column(name = "is_primary", nullable = false) private boolean primary;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public PayoutAccount(User user, String institutionCode, String institutionName,
                         String encryptedNumber, String maskedNumber, String holder, String alias, boolean primary) {
        this.user = user;
        this.institutionCode = institutionCode;
        this.institutionName = institutionName;
        this.accountNumberEncrypted = encryptedNumber;
        this.accountNumberMasked = maskedNumber;
        this.accountHolder = holder;
        this.alias = alias;
        this.primary = primary;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void setPrimary(boolean primary) { this.primary = primary; this.updatedAt = Instant.now(); }

    public void update(String institutionCode, String institutionName, String encryptedNumber,
                       String maskedNumber, String holder, String alias) {
        this.institutionCode = institutionCode;
        this.institutionName = institutionName;
        this.accountNumberEncrypted = encryptedNumber;
        this.accountNumberMasked = maskedNumber;
        this.accountHolder = holder;
        this.alias = alias;
        this.updatedAt = Instant.now();
    }
}
