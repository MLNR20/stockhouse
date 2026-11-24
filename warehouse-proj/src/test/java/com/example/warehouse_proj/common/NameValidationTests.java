package com.example.warehouse_proj.common;

import com.example.warehouse_proj.customer.CustomerDtos;
import com.example.warehouse_proj.employee.WarehouseEmployeeDtos;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests of the bean-validation rules: no Spring context and no database. */
class NameValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> customerErrors(String first, String last) {
        return validator.validate(new CustomerDtos.Request(first, last, "a@b.co", "+1-555-0100", "1 Main St"))
                .stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private Set<String> employeeErrors(String first, String last) {
        return validator.validate(new WarehouseEmployeeDtos.Request(UUID.randomUUID(), first, last, "a@b.co", "+1-555-0100"))
                .stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void acceptsLettersOnlyFromAnyAlphabet() {
        assertThat(customerErrors("Ava", "Johnson")).isEmpty();
        assertThat(customerErrors("Jo", "Li")).isEmpty(); // exactly 2 letters is fine
        assertThat(customerErrors("José", "Zoë")).isEmpty();
        assertThat(employeeErrors("Ava", "Johnson")).isEmpty();
        assertThat(employeeErrors("José", "Müller")).isEmpty();
    }

    @Test
    void rejectsNamesShorterThanTwoCharacters() {
        assertThat(customerErrors("A", "Johnson")).containsExactly("firstName");
        assertThat(customerErrors("Ava", "J")).containsExactly("lastName");
        assertThat(employeeErrors("A", "J")).containsExactlyInAnyOrder("firstName", "lastName");
    }

    @Test
    void rejectsDigitsSpacesAndPunctuation() {
        assertThat(customerErrors("Ava2", "Johnson")).containsExactly("firstName");
        assertThat(customerErrors("Mary Ann", "Johnson")).containsExactly("firstName");
        assertThat(customerErrors("Ava", "O'Brien")).containsExactly("lastName");
        assertThat(employeeErrors("Ava", "Smith-Jones")).containsExactly("lastName");
        assertThat(employeeErrors("Ava!", "Johnson")).containsExactly("firstName");
    }

    @Test
    void rejectsBlankAndMissingNames() {
        assertThat(customerErrors("", "Johnson")).contains("firstName");
        assertThat(customerErrors("   ", "Johnson")).contains("firstName");
        assertThat(customerErrors(null, null)).containsExactlyInAnyOrder("firstName", "lastName");
        assertThat(employeeErrors(null, "")).containsExactlyInAnyOrder("firstName", "lastName");
    }
}
