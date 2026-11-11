package com.nbbang.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter @Entity @Table(name = "room_participant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomParticipant {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id") private SettlementRoom room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false) private PaymentStatus paymentStatus;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    @Column(name = "paid_at") private Instant paidAt;
    public enum Role { HOST, MEMBER }
    public enum PaymentStatus { NOT_REQUIRED, PENDING, SENT, CONFIRMED }
    public RoomParticipant(SettlementRoom room, User user, Role role) {
        this.room = room; this.user = user; this.role = role;
        this.paymentStatus = role == Role.HOST ? PaymentStatus.NOT_REQUIRED : PaymentStatus.PENDING;
        this.joinedAt = Instant.now();
    }
    public void markSent() { if (role == Role.HOST) throw new IllegalArgumentException("방장은 송금 대상이 아닙니다."); this.paymentStatus = PaymentStatus.SENT; this.paidAt = Instant.now(); }
    public void confirm() { if (role == Role.HOST) throw new IllegalArgumentException("방장은 송금 대상이 아닙니다."); if (paymentStatus != PaymentStatus.SENT) throw new IllegalArgumentException("참가자의 송금 확인 요청이 먼저 필요합니다."); this.paymentStatus = PaymentStatus.CONFIRMED; if (this.paidAt == null) this.paidAt = Instant.now(); }
    public void confirmAnyway() { if (role == Role.MEMBER) { this.paymentStatus = PaymentStatus.CONFIRMED; if (this.paidAt == null) this.paidAt = Instant.now(); } }
    public void resetPaymentForAmountChange() { if (role == Role.MEMBER) { this.paymentStatus = PaymentStatus.PENDING; this.paidAt = null; } }
}
