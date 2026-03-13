package com.inventory.repository.specification;

import com.inventory.enums.Role;
import com.inventory.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

public class UserSpecification {

    private UserSpecification() {
        // Utility class
    }

    @NonNull
    public static Specification<User> withCriteria(@Nullable String search, @Nullable Role role, @Nullable Boolean enabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(search)) {
                @SuppressWarnings("null")
                String escaped = SpecificationUtils.escapeLikePattern(search.toLowerCase());
                String pattern = "%" + escaped + "%";
                predicates.add(cb.like(cb.lower(root.get("email")), pattern, SpecificationUtils.LIKE_ESCAPE_CHAR.charAt(0)));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
