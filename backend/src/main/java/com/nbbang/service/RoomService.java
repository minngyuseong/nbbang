package com.nbbang.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nbbang.domain.PayoutAccount;
import com.nbbang.domain.RoomParticipant;
import com.nbbang.domain.SettlementRoom;
import com.nbbang.domain.User;
import com.nbbang.repository.PayoutAccountRepository;
import com.nbbang.repository.RoomParticipantRepository;
import com.nbbang.repository.SettlementRoomRepository;
import com.nbbang.repository.UserRepository;

@Service
public class RoomService {
    private final UserRepository users;
    private final PayoutAccountRepository accounts;
    private final SettlementRoomRepository rooms;
    private final RoomParticipantRepository participants;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(UserRepository users, PayoutAccountRepository accounts,
                       SettlementRoomRepository rooms, RoomParticipantRepository participants) {
        this.users = users; this.accounts = accounts; this.rooms = rooms; this.participants = participants;
    }

    @Transactional
    public SettlementRoom create(UUID hostId, UUID accountId, String title, long totalAmount) {
        if (totalAmount <= 0) throw new IllegalArgumentException("총 결제 금액은 0보다 커야 합니다.");
        User host = users.findById(hostId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        PayoutAccount account = accounts.findByIdAndUserId(accountId, hostId)
                .orElseThrow(() -> new IllegalArgumentException("본인 소유 계좌만 선택할 수 있습니다."));
        SettlementRoom room = rooms.save(new SettlementRoom(newInviteToken(), title.trim(), host, account, totalAmount));
        participants.save(new RoomParticipant(room, host, RoomParticipant.Role.HOST));
        return room;
    }

    @Transactional
    public SettlementRoom join(String inviteToken, UUID userId) {
        SettlementRoom room = rooms.findByInviteTokenForUpdate(inviteToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대입니다."));
        if (room.getStatus() != SettlementRoom.Status.OPEN)
            throw new IllegalArgumentException("참여가 종료된 정산방입니다.");
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!participants.existsByRoomIdAndUserId(room.getId(), userId)) {
            participants.save(new RoomParticipant(room, user, RoomParticipant.Role.MEMBER));
        }
        return room;
    }

    @Transactional(readOnly = true)
    public List<RoomParticipant> history(UUID userId) { return participants.findHistory(userId); }

    private String newInviteToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
