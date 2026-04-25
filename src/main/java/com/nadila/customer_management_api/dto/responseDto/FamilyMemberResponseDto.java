package com.nadila.customer_management_api.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemberResponseDto {

    private Long id;
    private String name;
    private String nicNumber;
}
