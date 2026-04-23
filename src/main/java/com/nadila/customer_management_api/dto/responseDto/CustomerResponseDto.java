package com.nadila.customer_management_api.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {

    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private String nicNumber;
    private List<PhoneResponseDto> phoneNumbers;
    private List<AddressResponseDto> addresses;

}