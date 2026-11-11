package com.nbbang.api;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nbbang.security.NbbangPrincipal;
import com.nbbang.service.ExpenseService;
import com.nbbang.service.ExpenseService.RoomSnapshot;

@RestController
@RequestMapping("/api/rooms/{roomId}")
public class ExpenseController {
    private final ExpenseService expenses;
    public ExpenseController(ExpenseService expenses) { this.expenses = expenses; }

    @GetMapping
    public RoomSnapshot snapshot(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return expenses.snapshot(roomId, principal.userId());
    }

    @PutMapping("/amount")
    public RoomSnapshot setAmount(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId,
                                  @Valid @RequestBody AmountRequest request) {
        return expenses.setAmount(roomId, principal.userId(), request.amount());
    }

    @PostMapping("/payments/sent")
    public RoomSnapshot sent(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return expenses.markSent(roomId, principal.userId());
    }

    @PostMapping("/payments/{participantId}/confirm")
    public RoomSnapshot confirm(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId,
                                @PathVariable UUID participantId) {
        return expenses.confirm(roomId, participantId, principal.userId());
    }

    @PostMapping("/complete")
    public RoomSnapshot complete(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return expenses.complete(roomId, principal.userId());
    }

    @PostMapping("/complete-all")
    public RoomSnapshot completeAll(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return expenses.completeAll(roomId, principal.userId());
    }

    @PostMapping("/cancel")
    public RoomSnapshot cancel(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID roomId) {
        return expenses.cancel(roomId, principal.userId());
    }

    public record AmountRequest(@PositiveOrZero long amount) {}
}
