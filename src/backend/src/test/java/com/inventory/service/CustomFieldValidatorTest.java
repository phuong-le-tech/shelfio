package com.inventory.service;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.enums.CustomFieldType;
import com.inventory.exception.CustomFieldValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CustomFieldValidator Tests")
class CustomFieldValidatorTest {

    private CustomFieldValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CustomFieldValidator();
    }

    @Nested
    @DisplayName("validateDefinitionNames")
    class ValidateDefinitionNamesTests {

        @Test
        @DisplayName("should accept valid field names")
        void validNames_noException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0),
                    new CustomFieldDefinition("weight", "Weight", CustomFieldType.NUMBER, false, 1));

            assertThatCode(() -> validator.validateDefinitionNames(defs))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject reserved field name 'name'")
        void reservedName_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("name", "Name", CustomFieldType.TEXT, false, 0));

            assertThatThrownBy(() -> validator.validateDefinitionNames(defs))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("reserved field name");
        }

        @Test
        @DisplayName("should reject reserved field name 'status'")
        void reservedStatus_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("status", "Status", CustomFieldType.TEXT, false, 0));

            assertThatThrownBy(() -> validator.validateDefinitionNames(defs))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("reserved field name");
        }

        @Test
        @DisplayName("should reject duplicate field names")
        void duplicateNames_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0),
                    new CustomFieldDefinition("color", "Colour", CustomFieldType.TEXT, false, 1));

            assertThatThrownBy(() -> validator.validateDefinitionNames(defs))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("unique");
        }

        @Test
        @DisplayName("should accept null or empty definitions")
        void nullDefinitions_noException() {
            assertThatCode(() -> validator.validateDefinitionNames(null))
                    .doesNotThrowAnyException();
            assertThatCode(() -> validator.validateDefinitionNames(List.of()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("should accept valid TEXT value")
        void validText_noException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0));
            Map<String, Object> values = Map.of("color", "red");

            assertThatCode(() -> validator.validate(defs, values))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject non-string for TEXT field")
        void invalidText_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0));
            Map<String, Object> values = Map.of("color", 123);

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("text value");
        }

        @Test
        @DisplayName("should accept valid NUMBER value")
        void validNumber_noException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("weight", "Weight", CustomFieldType.NUMBER, false, 0));
            Map<String, Object> values = Map.of("weight", 42);

            assertThatCode(() -> validator.validate(defs, values))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject non-number for NUMBER field")
        void invalidNumber_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("weight", "Weight", CustomFieldType.NUMBER, false, 0));
            Map<String, Object> values = Map.of("weight", "heavy");

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("number");
        }

        @Test
        @DisplayName("should accept valid BOOLEAN value")
        void validBoolean_noException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("active", "Active", CustomFieldType.BOOLEAN, false, 0));
            Map<String, Object> values = Map.of("active", true);

            assertThatCode(() -> validator.validate(defs, values))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject non-boolean for BOOLEAN field")
        void invalidBoolean_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("active", "Active", CustomFieldType.BOOLEAN, false, 0));
            Map<String, Object> values = Map.of("active", "yes");

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("true or false");
        }

        @Test
        @DisplayName("should accept valid DATE value")
        void validDate_noException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("due", "Due Date", CustomFieldType.DATE, false, 0));
            Map<String, Object> values = Map.of("due", "2024-01-15");

            assertThatCode(() -> validator.validate(defs, values))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject invalid date format for DATE field")
        void invalidDateFormat_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("due", "Due Date", CustomFieldType.DATE, false, 0));
            Map<String, Object> values = Map.of("due", "15/01/2024");

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("valid date");
        }

        @Test
        @DisplayName("should reject non-string for DATE field")
        void nonStringDate_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("due", "Due Date", CustomFieldType.DATE, false, 0));
            Map<String, Object> values = Map.of("due", 20240115);

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("date string");
        }

        @Test
        @DisplayName("should throw when required field is missing")
        void requiredFieldMissing_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, true, 0));
            Map<String, Object> values = Map.of();

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("should throw when required field is blank")
        void requiredFieldBlank_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, true, 0));
            Map<String, Object> values = Map.of("color", "   ");

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("should throw for unknown custom field")
        void unknownField_throwsException() {
            List<CustomFieldDefinition> defs = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 0));
            Map<String, Object> values = Map.of("unknown_field", "value");

            assertThatThrownBy(() -> validator.validate(defs, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("Unknown custom field");
        }

        @Test
        @DisplayName("should throw when values provided but no definitions exist")
        void valuesWithNoDefinitions_throwsException() {
            Map<String, Object> values = Map.of("color", "red");

            assertThatThrownBy(() -> validator.validate(null, values))
                    .isInstanceOf(CustomFieldValidationException.class)
                    .hasMessageContaining("No custom fields are defined");
        }

        @Test
        @DisplayName("should accept null values when no definitions exist")
        void noDefinitionsNoValues_noException() {
            assertThatCode(() -> validator.validate(null, null))
                    .doesNotThrowAnyException();
        }
    }
}
