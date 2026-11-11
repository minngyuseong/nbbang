package com.nbbang.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.nbbang.domain.SettlementRoom;

public interface SettlementRoomRepository extends JpaRepository<SettlementRoom, UUID> {
    Optional<SettlementRoom> findByInviteToken(String inviteToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SettlementRoom r where r.id = :id")
    Optional<SettlementRoom> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SettlementRoom r where r.inviteToken = :inviteToken")
    Optional<SettlementRoom> findByInviteTokenForUpdate(@Param("inviteToken") String inviteToken);
}

