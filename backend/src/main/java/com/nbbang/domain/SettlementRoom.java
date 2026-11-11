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
@Table(name = "settlement_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementRoom {
    @Id @UuidGenerator private UUID id;
    @Column(name = "invite_token", nullable = false, unique = true, length = 100) private String inviteToken;
    @Column(nullable = false, length = 120) private String title;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "host_user_id") private User host;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "selected_account_id") private PayoutAccount selectedAccount;
    @Column(name = "account_institution_code", nullable = false) private String accountInstitutionCode;
    @Column(name = "account_institution_name", nullable = false) private String accountInstitutionName;
    @Column(name = "account_number_encrypted", nullable = false) private String accountNumberEncrypted;
    @Column(name = "account_number_masked", nullable = false) private String accountNumberMasked;
    @Column(name = "account_holder", nullable = false) private String accountHolder;
    @Column(name = "total_amount", nullable = false) private long totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "settled_at") private Instant settledAt;

    public enum Status { OPEN, SETTLING, COMPLETED, CANCELLED }

    public SettlementRoom(String inviteToken, String title, User host, PayoutAccount account, long totalAmount) {
        this.inviteToken = inviteToken;
        this.title = title;
        this.host = host;
        this.selectedAccount = account;
        this.accountInstitutionCode = account.getInstitutionCode();
        this.accountInstitutionName = account.getInstitutionName();
        this.accountNumberEncrypted = account.getAccountNumberEncrypted();
        this.accountNumberMasked = account.getAccountNumberMasked();
        this.accountHolder = account.getAccountHolder();
        this.totalAmount = totalAmount;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
    }
    public void complete() {
        if (status == Status.COMPLETED) return;
        if (status == Status.CANCELLED) throw new IllegalArgumentException("취소된 정산방입니다.");
        this.status = Status.COMPLETED;
        this.settledAt = Instant.now();
    }

    public void cancel() {
        if (status != Status.OPEN) throw new IllegalArgumentException("진행 중인 정산방만 취소할 수 있습니다.");
        this.status = Status.CANCELLED;
        this.settledAt = Instant.now();
    }
}
