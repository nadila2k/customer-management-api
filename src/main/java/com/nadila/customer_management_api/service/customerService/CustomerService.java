package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.CustomerDetailResponseDto;
import com.nadila.customer_management_api.dto.responseDto.CustomerResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CustomerService {

    CustomerResponseDto  createCustomer(CustomerRequestDto customerRequestDto);
    List<CustomerResponseDto> findAllCustomers();
    void deleteCustomer(Long customerId);
    CustomerResponseDto updateCustomer(Long customerId, CustomerRequestDto customerRequestDto);
    Page<CustomerResponseDto> getCustomersPaginated(int page, int size, String sortBy, String sortDirection);
    CustomerDetailResponseDto findCustomerById(Long customerId);
    List<CustomerResponseDto> searchCustomerByNameOrNic(String keyword);

}
