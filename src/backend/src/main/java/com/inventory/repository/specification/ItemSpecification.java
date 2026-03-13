package com.inventory.repository.specification;

import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.model.Item;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.springframework.util.StringUtils.hasText;

public class ItemSpecification {

    private ItemSpecification() {
        // Utility class
    }

    @NonNull
    public static Specification<Item> withCriteria(ItemSearchCriteria criteria, @Nullable UUID userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User-scope filter: non-admin users only see their own items
            if (userId != null) {
                predicates.add(cb.equal(root.get("itemList").get("user").get("id"), userId));
            }

            if (criteria != null) {
                if (hasText(criteria.search())) {
                    String escaped = SpecificationUtils.escapeLikePattern(criteria.search().toLowerCase());
                    String pattern = "%" + escaped + "%";
                    predicates.add(cb.like(cb.lower(root.get("name")), pattern, SpecificationUtils.LIKE_ESCAPE_CHAR.charAt(0)));
                }

                if (criteria.itemListId() != null) {
                    predicates.add(cb.equal(root.get("itemList").get("id"), criteria.itemListId()));
                }

                if (criteria.status() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.status()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
