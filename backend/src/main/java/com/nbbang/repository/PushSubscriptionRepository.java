package com.nbbang.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nbbang.domain.PushSubscription;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    Optional<PushSubscription> findByEndpoint(String endpoint);
    List<PushSubscription> findAllByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
