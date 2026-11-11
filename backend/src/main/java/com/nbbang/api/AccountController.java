package com.nbbang.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.nbbang.domain.PayoutAccount;
import com.nbbang.security.NbbangPrincipal;
import com.nbbang.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;
    public AccountController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal NbbangPrincipal principal) {
        return accounts.list(principal.userId()).stream().map(AccountResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@AuthenticationPrincipal NbbangPrincipal principal,
                                  @Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accounts.create(principal.userId(), request.institutionCode(), request.institutionName(),
                request.accountNumber(), request.accountHolder(), request.alias(), request.primary()));
    }

    @PutMapping("/{accountId}")
    public AccountResponse update(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID accountId,
                                  @Valid @RequestBody UpdateAccountRequest request) {
        return AccountResponse.from(accounts.update(principal.userId(), accountId, request.institutionCode(),
                request.accountNumber(), request.accountHolder(), request.alias()));
    }

    @PutMapping("/{accountId}/primary")
    public AccountResponse makePrimary(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID accountId) {
        return AccountResponse.from(accounts.makePrimary(principal.userId(), accountId));
    }

    @GetMapping("/{accountId}/number")
    public AccountNumberResponse number(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID accountId) {
        return new AccountNumberResponse(accounts.revealNumber(principal.userId(), accountId));
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal NbbangPrincipal principal, @PathVariable UUID accountId) {
        accounts.delete(principal.userId(), accountId);
    }

    public record CreateAccountRequest(@NotBlank String institutionCode, @NotBlank String institutionName,
            @NotBlank @Size(min = 8, max = 30) String accountNumber, @NotBlank String accountHolder,
            String alias, boolean primary) {}

    public record UpdateAccountRequest(@NotBlank String institutionCode,
            @NotBlank @Size(min = 8, max = 30) String accountNumber, @NotBlank String accountHolder,
            String alias) {}

    public record AccountNumberResponse(String accountNumber) {}

    public record AccountResponse(UUID id, String institutionCode, String institutionName, String maskedAccountNumber,
            String accountHolder, String alias, boolean primary) {
        static AccountResponse from(PayoutAccount account) {
            return new AccountResponse(account.getId(), account.getInstitutionCode(), account.getInstitutionName(),
                    account.getAccountNumberMasked(), account.getAccountHolder(), account.getAlias(), account.isPrimary());
        }
    }
}
