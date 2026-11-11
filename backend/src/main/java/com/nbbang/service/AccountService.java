package com.nbbang.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nbbang.domain.PayoutAccount;
import com.nbbang.domain.User;
import com.nbbang.repository.PayoutAccountRepository;
import com.nbbang.repository.UserRepository;

@Service
public class AccountService {
    private final UserRepository users;
    private final PayoutAccountRepository accounts;
    private final AccountCipher cipher;
    private final InstitutionCatalog institutions;

    public AccountService(UserRepository users, PayoutAccountRepository accounts, AccountCipher cipher,
                          InstitutionCatalog institutions) {
        this.users = users; this.accounts = accounts; this.cipher = cipher; this.institutions = institutions;
    }

    @Transactional(readOnly = true)
    public List<PayoutAccount> list(UUID userId) {
        return accounts.findAllByUserIdOrderByPrimaryDescCreatedAtDesc(userId);
    }

    @Transactional
    public PayoutAccount create(UUID userId, String institutionCode, String ignoredInstitutionName,
                                String accountNumber, String holder, String alias, boolean requestedPrimary) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        InstitutionCatalog.Entry institution = institutions.require(institutionCode);
        String normalized = normalizeAndValidate(institution, accountNumber);
        ensureUnique(userId, null, institution.code(), normalized);
        boolean primary = requestedPrimary || accounts.countByUserId(userId) == 0;
        if (primary) accounts.clearPrimary(userId);
        return accounts.save(new PayoutAccount(user, institution.code(), institution.name(), cipher.encrypt(normalized),
                mask(normalized), requireHolder(holder), normalizeAlias(alias), primary));
    }

    @Transactional
    public PayoutAccount makePrimary(UUID userId, UUID accountId) {
        PayoutAccount account = requireOwned(userId, accountId);
        accounts.clearPrimary(userId);
        account.setPrimary(true);
        return account;
    }

    @Transactional
    public PayoutAccount update(UUID userId, UUID accountId, String institutionCode,
                                String accountNumber, String holder, String alias) {
        PayoutAccount account = requireOwned(userId, accountId);
        InstitutionCatalog.Entry institution = institutions.require(institutionCode);
        String normalized = normalizeAndValidate(institution, accountNumber);
        ensureUnique(userId, accountId, institution.code(), normalized);
        account.update(institution.code(), institution.name(), cipher.encrypt(normalized), mask(normalized),
                requireHolder(holder), normalizeAlias(alias));
        return account;
    }

    @Transactional(readOnly = true)
    public String revealNumber(UUID userId, UUID accountId) {
        return cipher.decrypt(requireOwned(userId, accountId).getAccountNumberEncrypted());
    }

    @Transactional
    public void delete(UUID userId, UUID accountId) {
        PayoutAccount account = requireOwned(userId, accountId);
        boolean wasPrimary = account.isPrimary();
        accounts.delete(account);
        accounts.flush();
        if (wasPrimary) accounts.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(next -> next.setPrimary(true));
    }

    private PayoutAccount requireOwned(UUID userId, UUID accountId) {
        return accounts.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
    }

    private String normalizeAndValidate(InstitutionCatalog.Entry institution, String accountNumber) {
        String normalized = accountNumber == null ? "" : accountNumber.replaceAll("[^0-9]", "");
        int min = "BANK".equals(institution.category()) ? 10 : 8;
        int max = "BANK".equals(institution.category()) ? 16 : 20;
        if (normalized.length() < min || normalized.length() > max)
            throw new IllegalArgumentException("계좌번호는 " + min + "~" + max + "자리 숫자로 입력해 주세요.");
        return normalized;
    }

    private void ensureUnique(UUID userId, UUID excludedId, String institutionCode, String number) {
        boolean duplicate = accounts.findAllByUserIdOrderByPrimaryDescCreatedAtDesc(userId).stream()
                .filter(account -> excludedId == null || !account.getId().equals(excludedId))
                .anyMatch(account -> account.getInstitutionCode().equals(institutionCode)
                        && cipher.decrypt(account.getAccountNumberEncrypted()).equals(number));
        if (duplicate) throw new IllegalArgumentException("이미 등록된 계좌입니다.");
    }

    private String requireHolder(String holder) {
        String normalized = holder == null ? "" : holder.trim();
        if (normalized.isBlank() || normalized.length() > 100)
            throw new IllegalArgumentException("예금주를 올바르게 입력해 주세요.");
        return normalized;
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) return null;
        String normalized = alias.trim();
        if (normalized.length() > 100) throw new IllegalArgumentException("계좌 별칭은 100자 이하여야 합니다.");
        return normalized;
    }

    private String mask(String number) {
        int visible = Math.min(4, number.length());
        return "*".repeat(number.length() - visible) + number.substring(number.length() - visible);
    }
}
