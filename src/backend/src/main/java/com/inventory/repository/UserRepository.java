package com.inventory.repository;

import com.inventory.enums.Role;
import com.inventory.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    long countByRole(Role role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.stripeCustomerId = :customerId")
    Optional<User> findByStripeCustomerIdWithLock(@Param("customerId") String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.stripePaymentId = :paymentIntentId")
    Optional<User> findByStripePaymentIdWithLock(@Param("paymentIntentId") String paymentIntentId);

    long countByEnabled(boolean enabled);

    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) FROM User u WHERE u.createdAt >= :since GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> countUsersByMonth(@Param("since") LocalDateTime since);

    @Query("SELECT il.user.email, COUNT(il) FROM ItemList il GROUP BY il.user.email ORDER BY COUNT(il) DESC LIMIT 5")
    List<Object[]> findTopUsersByListCount();

    @Override
    @NonNull
    Page<User> findAll(@NonNull Pageable pageable);
}
