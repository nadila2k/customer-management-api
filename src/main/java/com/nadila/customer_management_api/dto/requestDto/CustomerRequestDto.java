package com.nadila.customer_management_api.dto.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @NotBlank(message = "NIC number is required")
    @Pattern(
            regexp = "^(\\d{9}[vVxX]|\\d{12})$",
            message = "NIC must be 9 digits + V/X or 12 digits"
    )
    private String nicNumber;

    @Valid
    private List<PhoneRequestDto> phones;

    @Valid
    private List<AddressRequestDto> addresses;

    @Size(max = 20, message = "Family member list cannot exceed 20 members")
    private List<@NotNull(message = "Family member ID must not be null")
    @Positive(message = "Family member ID must be a positive number") Long> familyMemberIds;
}