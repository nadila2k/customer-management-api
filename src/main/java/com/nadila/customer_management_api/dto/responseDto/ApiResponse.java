package com.nadila.customer_management_api.dto.responseDto;

import com.nadila.customer_management_api.enums.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private ResponseStatus status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}