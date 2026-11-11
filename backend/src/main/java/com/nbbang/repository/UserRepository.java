package com.nbbang.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nbbang.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderUserId(String provider, String providerUserId);
}

