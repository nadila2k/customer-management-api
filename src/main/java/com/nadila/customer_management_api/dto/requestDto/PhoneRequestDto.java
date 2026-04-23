package com.nadila.customer_management_api.dto.requestDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneRequestDto {

    @NotBlank(message = "Mobile number is required")
    @Size(max = 15)
    private String mobileNumber;
}
