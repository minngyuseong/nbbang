package com.nbbang.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nbbang.domain.ParticipantAmount;
import com.nbbang.domain.RoomParticipant;
import com.nbbang.domain.SettlementRoom;
import com.nbbang.repository.ParticipantAmountRepository;
import com.nbbang.repository.RoomParticipantRepository;
import com.nbbang.repository.SettlementRoomRepository;

@Service
public class ExpenseService {
    private final SettlementRoomRepository rooms;
    private final RoomParticipantRepository participants;
    private final ParticipantAmountRepository amounts;
    private final SimpMessagingTemplate messaging;
    private final String webOrigin;
    private final AccountCipher cipher;
    private final AuditService audit;
    private final WebPushService webPush;

    public ExpenseService(SettlementRoomRepository rooms, RoomParticipantRepository participants,
                          ParticipantAmountRepository amounts, SimpMessagingTemplate messaging,
                          @Value("${app.web-origin}") String webOrigin, AccountCipher cipher, AuditService audit,
                          WebPushService webPush) {
        this.rooms = rooms; this.participants = participants; this.amounts = amounts; this.messaging = messaging;
        this.webOrigin = webOrigin; this.cipher = cipher; this.audit = audit; this.webPush = webPush;
    }

    @Transactional(readOnly = true)
    public RoomSnapshot snapshot(UUID roomId, UUID userId) {
        requireParticipant(roomId, userId);
        return buildSnapshot(requireRoom(roomId));
    }

    @Transactional
    public RoomSnapshot setAmount(UUID roomId, UUID userId, long amount) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        RoomParticipant participant = requireParticipant(roomId, userId);
        if (amount < 0) throw new IllegalArgumentException("금액은 0원 이상이어야 합니다.");
        long otherAmounts = amounts.findRoomAmounts(roomId).stream()
                .filter(current -> !current.getParticipantId().equals(participant.getId()))
                .mapToLong(ParticipantAmount::getAmount).sum();
        if (otherAmounts + amount > room.getTotalAmount())
            throw new IllegalArgumentException("참가자별 금액 합계는 총 결제금액을 초과할 수 없습니다.");
        ParticipantAmount participantAmount = amounts.findByParticipantIdAndRoomId(participant.getId(), roomId)
                .orElseGet(() -> new ParticipantAmount(room, participant, 0));
        long before = participantAmount.getAmount();
        if (before != amount) participant.resetPaymentForAmountChange();
        participantAmount.update(amount);
        amounts.save(participantAmount);
        audit.record(room, userId, "PARTICIPANT_AMOUNT_UPDATED", "PARTICIPANT_AMOUNT", participant.getId(),
                Map.of("amount", before), Map.of("amount", amount));
        return publish(room);
    }

    @Transactional
    public RoomSnapshot markSent(UUID roomId, UUID userId) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        RoomParticipant participant = requireParticipant(roomId, userId);
        if (participant.getPaymentStatus() == RoomParticipant.PaymentStatus.SENT
                || participant.getPaymentStatus() == RoomParticipant.PaymentStatus.CONFIRMED) return buildSnapshot(room);
        participant.markSent();
        audit.record(room, userId, "PAYMENT_SENT", "ROOM_PARTICIPANT", participant.getId(), null,
                Map.of("paymentStatus", participant.getPaymentStatus().name()));
        webPush.send(room.getHost().getId(), "송금 확인 요청",
                participant.getUser().getDisplayName() + "님이 송금했다고 알려왔어요.", "/rooms/" + roomId);
        return publish(room);
    }

    @Transactional
    public RoomSnapshot confirm(UUID roomId, UUID participantId, UUID hostUserId) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        if (requireParticipant(roomId, hostUserId).getRole() != RoomParticipant.Role.HOST)
            throw new IllegalArgumentException("방장만 송금을 확인할 수 있습니다.");
        RoomParticipant target = participants.findByIdAndRoomId(participantId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));
        target.confirm();
        audit.record(room, hostUserId, "PAYMENT_CONFIRMED", "ROOM_PARTICIPANT", target.getId(), null,
                Map.of("paymentStatus", target.getPaymentStatus().name()));
        webPush.send(target.getUser().getId(), "정산 확인 완료", "방장이 송금을 확인했어요.", "/rooms/" + roomId);
        return publish(room);
    }

    @Transactional
    public RoomSnapshot complete(UUID roomId, UUID hostUserId) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        if (requireParticipant(roomId, hostUserId).getRole() != RoomParticipant.Role.HOST)
            throw new IllegalArgumentException("방장만 정산을 완료할 수 있습니다.");
        RoomSnapshot current = buildSnapshot(room);
        if (current.remainingAmount() != 0)
            throw new IllegalArgumentException("총 결제금액과 참가자별 금액 합계가 일치해야 합니다.");
        boolean pending = current.participants().stream()
                .anyMatch(participant -> !participant.role().equals("HOST") && !participant.paymentStatus().equals("CONFIRMED"));
        if (pending) throw new IllegalArgumentException("모든 참가자의 송금을 확인해 주세요.");
        room.complete();
        audit.record(room, hostUserId, "ROOM_COMPLETED", "SETTLEMENT_ROOM", room.getId(), null,
                Map.of("status", room.getStatus().name()));
        return publish(room);
    }

    @Transactional
    public RoomSnapshot completeAll(UUID roomId, UUID hostUserId) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        if (requireParticipant(roomId, hostUserId).getRole() != RoomParticipant.Role.HOST)
            throw new IllegalArgumentException("방장만 일괄 정산을 완료할 수 있습니다.");
        RoomSnapshot current = buildSnapshot(room);
        if (current.remainingAmount() != 0)
            throw new IllegalArgumentException("총 결제금액과 참가자별 금액 합계가 일치해야 합니다.");
        participants.findRoomParticipants(roomId).stream()
                .filter(participant -> participant.getRole() == RoomParticipant.Role.MEMBER)
                .forEach(participant -> {
                    participant.confirmAnyway();
                    webPush.send(participant.getUser().getId(), "정산 확인 완료",
                            "방장이 일괄 정산을 완료했어요.", "/rooms/" + roomId);
                });
        room.complete();
        audit.record(room, hostUserId, "ROOM_COMPLETED_ALL", "SETTLEMENT_ROOM", room.getId(), null,
                Map.of("status", room.getStatus().name()));
        return publish(room);
    }

    @Transactional
    public RoomSnapshot cancel(UUID roomId, UUID hostUserId) {
        SettlementRoom room = editableRoomForUpdate(roomId);
        if (requireParticipant(roomId, hostUserId).getRole() != RoomParticipant.Role.HOST)
            throw new IllegalArgumentException("방장만 정산방을 취소할 수 있습니다.");
        room.cancel();
        audit.record(room, hostUserId, "ROOM_CANCELLED", "SETTLEMENT_ROOM", room.getId(), null,
                Map.of("status", room.getStatus().name()));
        return publish(room);
    }

    private RoomSnapshot publish(SettlementRoom room) {
        RoomSnapshot snapshot = buildSnapshot(room);
        messaging.convertAndSend("/topic/rooms/" + room.getId(), snapshot);
        return snapshot;
    }

    private RoomSnapshot buildSnapshot(SettlementRoom room) {
        Map<UUID, ParticipantAmount> amountByParticipant = amounts.findRoomAmounts(room.getId()).stream()
                .collect(Collectors.toMap(ParticipantAmount::getParticipantId, Function.identity()));
        List<ParticipantView> participantViews = participants.findRoomParticipants(room.getId()).stream().map(participant -> {
            ParticipantAmount amount = amountByParticipant.get(participant.getId());
            return new ParticipantView(participant.getId(), participant.getUser().getId(), participant.getUser().getDisplayName(),
                    amount == null ? 0 : amount.getAmount(), participant.getRole().name(), participant.getPaymentStatus().name(),
                    amount == null ? 0 : amount.getVersion());
        }).toList();
        long allocated = participantViews.stream().mapToLong(ParticipantView::amount).sum();
        Recipient recipient = new Recipient(room.getAccountInstitutionCode(), room.getAccountInstitutionName(),
                cipher.decrypt(room.getAccountNumberEncrypted()), room.getAccountHolder());
        return new RoomSnapshot(room.getId(), webOrigin + "/join/" + room.getInviteToken(), room.getStatus().name(),
                recipient, room.getTotalAmount(), allocated, room.getTotalAmount() - allocated, participantViews, Instant.now());
    }

    private SettlementRoom requireRoom(UUID id) {
        return rooms.findById(id).orElseThrow(() -> new IllegalArgumentException("정산방을 찾을 수 없습니다."));
    }

    private SettlementRoom editableRoomForUpdate(UUID id) {
        SettlementRoom room = rooms.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("정산방을 찾을 수 없습니다."));
        if (room.getStatus() != SettlementRoom.Status.OPEN)
            throw new IllegalArgumentException("금액 입력이 종료된 정산방입니다.");
        return room;
    }

    private RoomParticipant requireParticipant(UUID roomId, UUID userId) {
        return participants.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("정산방 참가자만 이용할 수 있습니다."));
    }

    public record ParticipantView(UUID participantId, UUID userId, String userName, long amount,
                                  String role, String paymentStatus, long version) {}
    public record Recipient(String institutionCode, String institutionName, String accountNumber, String accountHolder) {}
    public record RoomSnapshot(UUID roomId, String inviteUrl, String status, Recipient recipient, long totalAmount,
                               long allocatedAmount, long remainingAmount, List<ParticipantView> participants,
                               Instant occurredAt) {}
}
