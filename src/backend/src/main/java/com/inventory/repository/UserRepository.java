package com.inventory.repository;

import com.inventory.enums.Role;
import com.inventory.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    long countByRole(Role role);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.stripeCustomerId = :customerId")
    Optional<User> findByStripeCustomerIdWithLock(@Param("customerId") String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.stripePaymentId = :paymentIntentId")
    Optional<User> findByStripePaymentIdWithLock(@Param("paymentIntentId") String paymentIntentId);

    @Override
    @NonNull
    Page<User> findAll(@NonNull Pageable pageable);
}
