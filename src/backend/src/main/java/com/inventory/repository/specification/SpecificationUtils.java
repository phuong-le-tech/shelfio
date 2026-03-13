package com.inventory.repository.specification;

public final class SpecificationUtils {

    static final String LIKE_ESCAPE_CHAR = "\\";

    private SpecificationUtils() {
        // Utility class
    }

    static String escapeLikePattern(String input) {
        return input
                .replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
                .replace("%", LIKE_ESCAPE_CHAR + "%")
                .replace("_", LIKE_ESCAPE_CHAR + "_");
    }
}
