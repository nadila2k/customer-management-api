package com.nadila.customer_management_api.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDetailResponseDto {

    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private String nicNumber;
    private List<PhoneResponseDto> phoneNumbers;
    private List<AddressResponseDto> addresses;
    private List<FamilyMemberResponseDto> familyMembers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
