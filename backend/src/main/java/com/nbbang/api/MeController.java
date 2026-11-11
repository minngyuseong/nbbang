package com.nbbang.api;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import com.nbbang.security.NbbangPrincipal;
import com.nbbang.service.UserService;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserService users;

    public MeController(UserService users) { this.users = users; }

    @GetMapping
    MeResponse me(@AuthenticationPrincipal NbbangPrincipal principal) {
        return new MeResponse(principal.userId(), principal.name());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(@AuthenticationPrincipal NbbangPrincipal principal, HttpServletRequest request) {
        users.withdraw(principal.userId());
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }
    record MeResponse(UUID id, String name) {}
}
