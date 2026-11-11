package com.nbbang.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nbbang.domain.RoomParticipant;
import com.nbbang.domain.SettlementRoom;
import com.nbbang.security.NbbangPrincipal;
import com.nbbang.service.RoomService;
import com.nbbang.service.AuditService;
import com.nbbang.domain.SettlementAuditLog;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService rooms;
    private final String webOrigin;
    private final AuditService audit;
    public RoomController(RoomService rooms, AuditService audit, @Value("${app.web-origin}") String webOrigin) {
        this.rooms = rooms; this.audit = audit; this.webOrigin = webOrigin;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@AuthenticationPrincipal NbbangPrincipal principal, @Valid @RequestBody CreateRoomRequest request) {
        return response(rooms.create(principal.userId(), request.accountId(), request.title(), request.totalAmount()));
    }

    @PostMapping("/join/{inviteToken}")
    public RoomResponse join(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable String inviteToken) {
        return response(rooms.join(inviteToken, principal.userId()));
    }

    @GetMapping("/history")
    public List<HistoryResponse> history(@AuthenticationPrincipal NbbangPrincipal principal) {
        return rooms.history(principal.userId()).stream().map(p -> new HistoryResponse(p.getRoom().getId(),
                p.getRoom().getTitle(), p.getRoom().getTotalAmount(), p.getRoom().getStatus().name(),
                p.getRole().name(), p.getPaymentStatus().name(), p.getRoom().getCreatedAt())).toList();
    }

    @GetMapping("/{roomId}/audit")
    public List<AuditResponse> audit(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return audit.list(roomId, principal.userId()).stream().map(AuditResponse::from).toList();
    }

    private RoomResponse response(SettlementRoom room) {
        return new RoomResponse(room.getId(), room.getTitle(), room.getTotalAmount(), room.getStatus().name(),
                room.getAccountInstitutionName(), room.getAccountNumberMasked(), room.getAccountHolder(),
                webOrigin + "/join/" + room.getInviteToken());
    }

    public record CreateRoomRequest(@NotBlank @Size(max = 120) String title, @Positive long totalAmount, UUID accountId) {}
    public record RoomResponse(UUID id, String title, long totalAmount, String status, String institutionName,
            String maskedAccountNumber, String accountHolder, String inviteUrl) {}
    public record HistoryResponse(UUID id, String title, long totalAmount, String status, String role,
            String paymentStatus, Instant createdAt) {}
    public record AuditResponse(String eventType, String actorName, Map<String,Object> beforeData,
                                Map<String,Object> afterData, Instant createdAt) {
        static AuditResponse from(SettlementAuditLog log) { return new AuditResponse(log.getEventType(),
                log.getActor().getDisplayName(), log.getBeforeData(), log.getAfterData(), log.getCreatedAt()); }
    }
}
