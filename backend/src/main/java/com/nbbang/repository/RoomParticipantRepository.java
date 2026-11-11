package com.nbbang.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.nbbang.domain.RoomParticipant;
import com.nbbang.domain.SettlementRoom;
import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    Optional<RoomParticipant> findByRoomIdAndUserId(UUID roomId, UUID userId);
    Optional<RoomParticipant> findByIdAndRoomId(UUID id, UUID roomId);

    @Query("select p from RoomParticipant p join fetch p.user where p.room.id = :roomId order by p.joinedAt")
    List<RoomParticipant> findRoomParticipants(UUID roomId);

    @Query("select p from RoomParticipant p join fetch p.room r where p.user.id = :userId order by r.createdAt desc")
    List<RoomParticipant> findHistory(UUID userId);

    boolean existsByUser_IdAndRoom_Status(UUID userId, SettlementRoom.Status status);
}
