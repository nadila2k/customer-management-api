package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.*;
import com.nadila.customer_management_api.entity.*;
import com.nadila.customer_management_api.exception.*;
import com.nadila.customer_management_api.repository.CityRepository;
import com.nadila.customer_management_api.repository.CustomerRepository;
import com.nadila.customer_management_api.service.cityService.CityService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CityService cityService;
    private final ModelMapper modelMapper;

    @Override

    public CustomerResponseDto createCustomer(CustomerRequestDto customerRequestDto) {

        return Optional.ofNullable(customerRequestDto)
                .filter(req -> !customerRepository.existsByNicNumber(req.getNicNumber()))
                .map(req -> {
                    Customer customer = modelMapper.map(req, Customer.class);

                    List<CustomerPhone> phones = req.getPhones().stream()
                            .map(p -> CustomerPhone.builder()
                                    .customer(customer)
                                    .mobileNumber(p.getMobileNumber())
                                    .build())
                            .collect(Collectors.toList());
                    customer.setPhones(phones);

                    // ✅ cityService — from cache
                    List<CustomerAddress> addresses = req.getAddresses().stream()
                            .map(a -> {
                                City city = cityService.getCityByName(a.getCityName());
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

                        List<Long> uniqueIds = req.getFamilyMemberIds().stream().distinct().toList();
                        if (uniqueIds.size() != req.getFamilyMemberIds().size()) {
                            throw new DuplicateFamilyMemberException(
                                    "Duplicate family member IDs are not allowed");
                        }

                        if (req.getFamilyMemberIds().contains(customer.getId())) {
                            throw new SelfFamilyReferenceException(
                                    "Customer cannot be their own family member");
                        }

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

    @Override
    public List<CustomerResponseDto> findAllCustomers() {

        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public void deleteCustomer(Long customerId) {
        customerRepository.findById(customerId)
                .ifPresentOrElse(
                        customerRepository::delete,
                        () -> { throw new ResourceNotFoundException("Customer with ID: " + customerId + " not found"); }
                );
    }

    @Override
    public CustomerResponseDto updateCustomer(Long customerId, CustomerRequestDto req) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        if (!customer.getNicNumber().equals(req.getNicNumber()) &&
                customerRepository.existsByNicNumber(req.getNicNumber())) {
            throw new DuplicateResourceException(
                    "NIC number already in use: " + req.getNicNumber());
        }

        customer.setName(req.getName());
        customer.setDateOfBirth(req.getDateOfBirth());
        customer.setNicNumber(req.getNicNumber());

        List<CustomerPhone> phones = req.getPhones().stream()
                .map(p -> CustomerPhone.builder()
                        .customer(customer)
                        .mobileNumber(p.getMobileNumber())
                        .build())
                .collect(Collectors.toList());
        customer.getPhones().clear();
        customer.getPhones().addAll(phones);

        // ✅ cityService — from cache
        List<CustomerAddress> addresses = req.getAddresses().stream()
                .map(a -> {
                    City city = cityService.getCityByName(a.getCityName());
                    return CustomerAddress.builder()
                            .customer(customer)
                            .addressLine1(a.getAddressLine1())
                            .addressLine2(a.getAddressLine2())
                            .city(city)
                            .build();
                })
                .collect(Collectors.toList());
        customer.getAddresses().clear();
        customer.getAddresses().addAll(addresses);

        customer.getFamilyLinks().clear();
        if (req.getFamilyMemberIds() != null && !req.getFamilyMemberIds().isEmpty()) {

            List<Long> uniqueIds = req.getFamilyMemberIds().stream().distinct().toList();
            if (uniqueIds.size() != req.getFamilyMemberIds().size()) {
                throw new DuplicateFamilyMemberException(
                        "Duplicate family member IDs are not allowed");
            }

            if (req.getFamilyMemberIds().contains(customerId)) {
                throw new SelfFamilyReferenceException(
                        "Customer cannot be their own family member");
            }

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
            customer.getFamilyLinks().addAll(familyLinks);
        }

        Customer updated = customerRepository.save(customer);
        return toResponseDto(updated);
    }


    @Override
    public Page<CustomerResponseDto> findAllCustomers(int page, int size, String sortBy, String sortDirection) {
        List<String> allowedSortFields = List.of("name", "dateOfBirth", "nicNumber", "createdAt", "updatedAt");
        if (!allowedSortFields.contains(sortBy)) {
            throw new InvalidRequestException(
                    "Invalid sort field: " + sortBy + ". Allowed: " + allowedSortFields);
        }


        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return customerRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    @Override
    public CustomerDetailResponseDto findCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        return toDetailResponseDto(customer);
    }

    private CustomerDetailResponseDto toDetailResponseDto(Customer customer) {


        List<PhoneResponseDto> phoneNumbers = Optional.ofNullable(customer.getPhones())
                .orElse(List.of())
                .stream()
                .map(p -> PhoneResponseDto.builder()
                        .id(p.getId())
                        .mobileNumber(p.getMobileNumber())
                        .build())
                .toList();


        List<AddressResponseDto> addresses = Optional.ofNullable(customer.getAddresses())
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


        List<FamilyMemberResponseDto> familyMembers = Optional.ofNullable(customer.getFamilyLinks())
                .orElse(List.of())
                .stream()
                .map(link -> FamilyMemberResponseDto.builder()
                        .id(link.getFamilyMember().getId())
                        .name(link.getFamilyMember().getName())
                        .nicNumber(link.getFamilyMember().getNicNumber())
                        .build())
                .toList();

        return CustomerDetailResponseDto.builder()
                .id(customer.getId())
                .name(customer.getName())
                .dateOfBirth(customer.getDateOfBirth())
                .nicNumber(customer.getNicNumber())
                .phoneNumbers(phoneNumbers)
                .addresses(addresses)
                .familyMembers(familyMembers)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
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
