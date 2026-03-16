package com.inventory.repository;

import com.inventory.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    @Query("SELECT i.status, COUNT(i) FROM Item i GROUP BY i.status")
    List<Object[]> countByStatus();

    @Query("SELECT i.itemList.category, COUNT(i) FROM Item i GROUP BY i.itemList.category")
    List<Object[]> countByCategory();

    @Query("SELECT COUNT(i) FROM Item i WHERE i.itemList.user.id = :userId")
    long countByUserId(UUID userId);

    @Query("SELECT i.status, COUNT(i) FROM Item i WHERE i.itemList.user.id = :userId GROUP BY i.status")
    List<Object[]> countByStatusAndUserId(UUID userId);

    @Query("SELECT i.itemList.category, COUNT(i) FROM Item i WHERE i.itemList.user.id = :userId GROUP BY i.itemList.category")
    List<Object[]> countByCategoryAndUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(i.stock), 0) FROM Item i")
    long sumStock();

    @Query("SELECT COALESCE(SUM(i.stock), 0) FROM Item i WHERE i.itemList.user.id = :userId")
    long sumStockByUserId(UUID userId);

@Query("SELECT i.itemList.name, COUNT(i), COALESCE(SUM(i.stock), 0) FROM Item i GROUP BY i.itemList.name")
    List<Object[]> getListsOverview();

    @Query("SELECT i.itemList.name, COUNT(i), COALESCE(SUM(i.stock), 0) FROM Item i WHERE i.itemList.user.id = :userId GROUP BY i.itemList.name")
    List<Object[]> getListsOverviewByUserId(UUID userId);

    @Query("SELECT i FROM Item i JOIN FETCH i.itemList ORDER BY i.updatedAt DESC LIMIT 5")
    List<Item> findTop5ByOrderByUpdatedAtDesc();

    @Query("SELECT i FROM Item i JOIN FETCH i.itemList WHERE i.itemList.user.id = :userId ORDER BY i.updatedAt DESC LIMIT 5")
    List<Item> findTop5ByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Query("SELECT i.itemList.user.email, COUNT(i) FROM Item i GROUP BY i.itemList.user.email ORDER BY COUNT(i) DESC LIMIT 5")
    List<Object[]> findTopUsersByItemCount();

    long countByItemListId(UUID itemListId);

    List<Item> findAllByItemListIdOrderByCreatedAtAsc(UUID itemListId);

}
