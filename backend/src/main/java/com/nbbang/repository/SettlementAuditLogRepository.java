package com.nbbang.repository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nbbang.domain.SettlementAuditLog;
public interface SettlementAuditLogRepository extends JpaRepository<SettlementAuditLog, UUID> {
    @Query("select l from SettlementAuditLog l join fetch l.actor where l.room.id = :roomId order by l.createdAt desc")
    List<SettlementAuditLog> findRoomLogs(UUID roomId);
}
