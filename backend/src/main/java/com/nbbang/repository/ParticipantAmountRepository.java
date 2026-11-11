package com.nbbang.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.nbbang.domain.ParticipantAmount;

public interface ParticipantAmountRepository extends JpaRepository<ParticipantAmount, UUID> {
    Optional<ParticipantAmount> findByParticipantIdAndRoomId(UUID participantId, UUID roomId);

    @Query("select a from ParticipantAmount a join fetch a.participant p join fetch p.user where a.room.id = :roomId")
    List<ParticipantAmount> findRoomAmounts(UUID roomId);
}
