package com.nbbang.service;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.nbbang.domain.SettlementAuditLog;
import com.nbbang.domain.SettlementRoom;
import com.nbbang.domain.User;
import com.nbbang.repository.SettlementAuditLogRepository;
import com.nbbang.repository.UserRepository;
import com.nbbang.repository.RoomParticipantRepository;
@Service
public class AuditService {
    private final SettlementAuditLogRepository logs; private final UserRepository users; private final RoomParticipantRepository participants;
    public AuditService(SettlementAuditLogRepository logs,UserRepository users,RoomParticipantRepository participants){this.logs=logs;this.users=users;this.participants=participants;}
    public void record(SettlementRoom room, UUID actorId, String event, String entity, UUID entityId, Map<String,Object> before, Map<String,Object> after){User actor=users.getReferenceById(actorId);logs.save(new SettlementAuditLog(room,actor,event,entity,entityId,before,after));}
    public List<SettlementAuditLog> list(UUID roomId, UUID userId){if(!participants.existsByRoomIdAndUserId(roomId,userId))throw new IllegalArgumentException("정산방 참가자만 변경 이력을 볼 수 있습니다.");return logs.findRoomLogs(roomId);}
}
