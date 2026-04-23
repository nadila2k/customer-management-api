package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.CustomerResponseDto;

import java.util.List;

public interface CustomerService {

    CustomerResponseDto  createCustomer(CustomerRequestDto customerRequestDto);

}
