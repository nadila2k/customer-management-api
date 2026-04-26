package com.nadila.customer_management_api.dto.responseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkErrorEntryDto {
    private int    row;
    private String message;
}