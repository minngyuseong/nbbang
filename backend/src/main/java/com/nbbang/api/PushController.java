package com.nbbang.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nbbang.security.NbbangPrincipal;
import com.nbbang.service.WebPushService;

@RestController
@RequestMapping("/api/push")
public class PushController {
    private final WebPushService push;
    public PushController(WebPushService push) { this.push = push; }

    @GetMapping("/public-key")
    public PublicKeyResponse publicKey() { return new PublicKeyResponse(push.publicKey()); }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal NbbangPrincipal principal, @Valid @RequestBody SubscriptionRequest request) {
        push.subscribe(principal.userId(), request.endpoint(), request.keys().p256dh(), request.keys().auth());
    }

    public record PublicKeyResponse(String publicKey) {}
    public record Keys(@NotBlank String p256dh, @NotBlank String auth) {}
    public record SubscriptionRequest(@NotBlank String endpoint, @Valid Keys keys) {}
}
