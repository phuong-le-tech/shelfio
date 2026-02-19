package com.inventory.service;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.exception.CustomFieldValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class CustomFieldValidator {

    private static final Set<String> RESERVED_NAMES = Set.of(
            "name", "status", "stock", "image", "id", "created_at", "updated_at");

    public void validate(List<CustomFieldDefinition> definitions, Map<String, Object> values) {
        if (definitions == null || definitions.isEmpty()) {
            if (values != null && !values.isEmpty()) {
                throw new CustomFieldValidationException(
                        "No custom fields are defined for this list");
            }
            return;
        }

        Map<String, Object> safeValues = values != null ? values : Map.of();

        for (CustomFieldDefinition def : definitions) {
            Object value = safeValues.get(def.name());
            if (def.required() && (value == null || isBlank(value))) {
                throw new CustomFieldValidationException(
                        "Custom field '" + def.label() + "' is required");
            }
            if (value != null && !isBlank(value)) {
                validateType(def, value);
            }
        }

        for (String key : safeValues.keySet()) {
            boolean found = definitions.stream()
                    .anyMatch(d -> d.name().equals(key));
            if (!found) {
                throw new CustomFieldValidationException(
                        "Unknown custom field: '" + key + "'");
            }
        }
    }

    public void validateDefinitionNames(List<CustomFieldDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (CustomFieldDefinition def : definitions) {
            if (RESERVED_NAMES.contains(def.name().toLowerCase())) {
                throw new CustomFieldValidationException(
                        "'" + def.name() + "' is a reserved field name and cannot be used as a custom field");
            }
        }

        long uniqueNames = definitions.stream()
                .map(CustomFieldDefinition::name)
                .distinct()
                .count();
        if (uniqueNames < definitions.size()) {
            throw new CustomFieldValidationException(
                    "Custom field names must be unique within a list");
        }
    }

    private void validateType(CustomFieldDefinition def, Object value) {
        switch (def.type()) {
            case TEXT -> {
                if (!(value instanceof String)) {
                    throw new CustomFieldValidationException(
                            "Field '" + def.label() + "' must be a text value");
                }
            }
            case NUMBER -> {
                if (!(value instanceof Number)) {
                    throw new CustomFieldValidationException(
                            "Field '" + def.label() + "' must be a number");
                }
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    throw new CustomFieldValidationException(
                            "Field '" + def.label() + "' must be true or false");
                }
            }
            case DATE -> {
                if (!(value instanceof String)) {
                    throw new CustomFieldValidationException(
                            "Field '" + def.label() + "' must be a date string (YYYY-MM-DD)");
                }
                try {
                    LocalDate.parse((String) value);
                } catch (DateTimeParseException e) {
                    throw new CustomFieldValidationException(
                            "Field '" + def.label() + "' must be a valid date (YYYY-MM-DD)");
                }
            }
        }
    }

    private boolean isBlank(Object value) {
        return value instanceof String s && s.isBlank();
    }
}
