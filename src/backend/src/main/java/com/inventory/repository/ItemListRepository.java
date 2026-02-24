package com.inventory.repository;

import com.inventory.model.ItemList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemListRepository extends JpaRepository<ItemList, UUID>, JpaSpecificationExecutor<ItemList> {

    @Query("SELECT il FROM ItemList il LEFT JOIN FETCH il.items WHERE il.id = :id")
    Optional<ItemList> findByIdWithItems(@NonNull UUID id);

    // User-filtered queries for multi-tenancy
    Page<ItemList> findByUserId(@NonNull UUID userId, @NonNull Pageable pageable);

    @Query("SELECT il FROM ItemList il LEFT JOIN FETCH il.items WHERE il.id = :id AND il.user.id = :userId")
    Optional<ItemList> findByIdAndUserIdWithItems(@NonNull UUID id, @NonNull UUID userId);

    @Query("SELECT il FROM ItemList il WHERE il.id = :id AND il.user.id = :userId")
    Optional<ItemList> findByIdAndUserId(@NonNull UUID id, @NonNull UUID userId);

    boolean existsByIdAndUserId(@NonNull UUID id, @NonNull UUID userId);
}
