package com.inventory.repository;

import com.inventory.model.ItemList;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemListRepository extends JpaRepository<ItemList, UUID>, JpaSpecificationExecutor<ItemList> {

    // User-filtered queries for multi-tenancy
    Page<ItemList> findByUserId(@NonNull UUID userId, @NonNull Pageable pageable);

    @Query("SELECT il FROM ItemList il WHERE il.id = :id AND il.user.id = :userId")
    Optional<ItemList> findByIdAndUserId(@NonNull UUID id, @NonNull UUID userId);

    boolean existsByIdAndUserId(@NonNull UUID id, @NonNull UUID userId);

    long countByUserId(@NonNull UUID userId);

    @Query("SELECT il.id, COUNT(i) FROM ItemList il LEFT JOIN il.items i WHERE il.id IN :ids GROUP BY il.id")
    List<Object[]> countItemsByListIds(@Param("ids") List<UUID> ids);

    // Workspace-scoped queries
    Page<ItemList> findByWorkspaceId(@NonNull UUID workspaceId, @NonNull Pageable pageable);

    @Query("SELECT il FROM ItemList il WHERE il.id = :id AND il.workspace.id IN :workspaceIds")
    Optional<ItemList> findByIdAndWorkspaceIdIn(@NonNull UUID id, @NonNull List<UUID> workspaceIds);

    long countByWorkspaceId(@NonNull UUID workspaceId);

    @Query("SELECT il.workspace.id, COUNT(il) FROM ItemList il WHERE il.workspace.id IN :workspaceIds GROUP BY il.workspace.id")
    List<Object[]> countListsByWorkspaceIds(@Param("workspaceIds") List<UUID> workspaceIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT il FROM ItemList il WHERE il.id = :id")
    Optional<ItemList> findByIdWithLock(@Param("id") UUID id);
}
