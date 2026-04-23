package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.AddressResponseDto;
import com.nadila.customer_management_api.dto.responseDto.CustomerResponseDto;
import com.nadila.customer_management_api.dto.responseDto.PhoneResponseDto;
import com.nadila.customer_management_api.entity.*;
import com.nadila.customer_management_api.exception.DuplicateResourceException;
import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.CityRepository;
import com.nadila.customer_management_api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CityRepository cityRepository;
    private final ModelMapper modelMapper;

    @Override
    public CustomerResponseDto createCustomer(CustomerRequestDto customerRequestDto) {

        return Optional.ofNullable(customerRequestDto)
                .filter(req -> !customerRepository.existsByNicNumber(req.getNicNumber()))
                .map( req -> {
                    Customer customer = modelMapper.map(req, Customer.class);

                    List<CustomerPhone> phones = req.getPhones().stream()
                            .map(p -> CustomerPhone.builder()
                                    .customer(customer)
                                    .mobileNumber(p.getMobileNumber())
                                    .build())
                            .collect(Collectors.toList());
                    customer.setPhones(phones);

                    List<CustomerAddress> addresses = req.getAddresses().stream()
                            .map(a -> {
                                City city = (City) cityRepository.findByNameIgnoreCase(a.getCityName())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                "City not found: " + a.getCityName()));
                                return CustomerAddress.builder()
                                        .customer(customer)
                                        .addressLine1(a.getAddressLine1())
                                        .addressLine2(a.getAddressLine2())
                                        .city(city)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    customer.setAddresses(addresses);

                    if (req.getFamilyMemberIds() != null && !req.getFamilyMemberIds().isEmpty()) {
                        List<CustomerFamily> familyLinks = req.getFamilyMemberIds().stream()
                                .map(memberId -> {
                                    Customer familyMember = customerRepository.findById(memberId)
                                            .orElseThrow(() -> new ResourceNotFoundException(
                                                    "Family member not found with ID: " + memberId));
                                    return CustomerFamily.builder()
                                            .customer(customer)
                                            .familyMember(familyMember)
                                            .build();
                                })
                                .collect(Collectors.toList());
                        customer.setFamilyLinks(familyLinks);
                    }

                    Customer saved = customerRepository.save(customer);

                    return toResponseDto(saved);

                })
                .orElseThrow(() -> new DuplicateResourceException(
                        "Customer already exists with NIC number: " + customerRequestDto.getNicNumber()));
    }


    private CustomerResponseDto toResponseDto(Customer saved) {

        CustomerResponseDto dto = modelMapper.map(saved, CustomerResponseDto.class);

        List<PhoneResponseDto> phoneNumbers = Optional.ofNullable(saved.getPhones())
                .orElse(List.of())
                .stream()
                .map(p -> PhoneResponseDto.builder()
                        .id(p.getId())
                        .mobileNumber(p.getMobileNumber())
                        .build())
                .toList();

        List<AddressResponseDto> addresses = Optional.ofNullable(saved.getAddresses())
                .orElse(List.of())
                .stream()
                .map(a -> AddressResponseDto.builder()
                        .id(a.getId())
                        .addressLine1(a.getAddressLine1())
                        .addressLine2(a.getAddressLine2())
                        .cityName(a.getCity().getName())
                        .countryName(a.getCity().getCountry().getName())
                        .build())
                .toList();

        dto.setPhoneNumbers(phoneNumbers);
        dto.setAddresses(addresses);

        return dto;
    }
}
