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
}
