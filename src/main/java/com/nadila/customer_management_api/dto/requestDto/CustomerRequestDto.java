package com.nadila.customer_management_api.dto.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequestDto {

    @NotBlank(message = "Name is mandatory")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Date of birth is mandatory")
    private LocalDate dateOfBirth;

    @NotBlank(message = "NIC number is mandatory")
    @Size(max = 20)
    private String nicNumber;

    private Long primaryCustomerId;

    private List<PhoneRequestDto> phones;

    private List<AddressRequestDto> addresses;
}
