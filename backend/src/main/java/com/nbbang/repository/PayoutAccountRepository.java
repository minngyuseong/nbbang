package com.nbbang.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.nbbang.domain.PayoutAccount;

public interface PayoutAccountRepository extends JpaRepository<PayoutAccount, UUID> {
    List<PayoutAccount> findAllByUserIdOrderByPrimaryDescCreatedAtDesc(UUID userId);
    Optional<PayoutAccount> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
    Optional<PayoutAccount> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteAllByUserId(UUID userId);

    @Modifying
    @Query("update PayoutAccount a set a.primary = false where a.user.id = :userId and a.primary = true")
    void clearPrimary(UUID userId);
}
