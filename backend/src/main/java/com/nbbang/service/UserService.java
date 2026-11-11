package com.nbbang.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nbbang.domain.User;
import com.nbbang.domain.SettlementRoom;
import com.nbbang.repository.PayoutAccountRepository;
import com.nbbang.repository.PushSubscriptionRepository;
import com.nbbang.repository.RoomParticipantRepository;
import com.nbbang.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository users;
    private final PayoutAccountRepository accounts;
    private final PushSubscriptionRepository subscriptions;
    private final RoomParticipantRepository participants;

    public UserService(UserRepository users, PayoutAccountRepository accounts,
                       PushSubscriptionRepository subscriptions, RoomParticipantRepository participants) {
        this.users = users;
        this.accounts = accounts;
        this.subscriptions = subscriptions;
        this.participants = participants;
    }

    @Transactional
    public void withdraw(UUID userId) {
        if (participants.existsByUser_IdAndRoom_Status(userId, SettlementRoom.Status.OPEN))
            throw new IllegalArgumentException("진행 중인 정산을 완료하거나 취소한 뒤 탈퇴해 주세요.");
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        subscriptions.deleteAllByUserId(userId);
        accounts.deleteAllByUserId(userId);
        user.anonymize();
    }
}
