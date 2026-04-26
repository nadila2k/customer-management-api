package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.responseDto.*;
import com.nadila.customer_management_api.entity.*;
import com.nadila.customer_management_api.exception.*;
import com.nadila.customer_management_api.repository.CustomerRepository;
import com.nadila.customer_management_api.service.cityService.CityService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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


                    if (req.getPhones() != null && !req.getPhones().isEmpty()) {
                        List<CustomerPhone> phones = req.getPhones().stream()
                                .map(p -> CustomerPhone.builder()
                                        .customer(customer)
                                        .mobileNumber(p.getMobileNumber())
                                        .build())
                                .collect(Collectors.toList());
                        customer.setPhones(phones);
                    }


                    if (req.getAddresses() != null && !req.getAddresses().isEmpty()) {
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
                    }


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

        // phones — optional
        if (customer.getPhones() != null) customer.getPhones().clear();
        if (req.getPhones() != null && !req.getPhones().isEmpty()) {
            List<CustomerPhone> phones = req.getPhones().stream()
                    .map(p -> CustomerPhone.builder()
                            .customer(customer)
                            .mobileNumber(p.getMobileNumber())
                            .build())
                    .collect(Collectors.toList());
            if (customer.getPhones() == null) {
                customer.setPhones(phones);
            } else {
                customer.getPhones().addAll(phones);
            }
        }


        if (customer.getAddresses() != null) customer.getAddresses().clear();
        if (req.getAddresses() != null && !req.getAddresses().isEmpty()) {
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
            if (customer.getAddresses() == null) {
                customer.setAddresses(addresses);
            } else {
                customer.getAddresses().addAll(addresses);
            }
        }


        if (customer.getFamilyLinks() != null) customer.getFamilyLinks().clear();
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
            if (customer.getFamilyLinks() == null) {
                customer.setFamilyLinks(familyLinks);
            } else {
                customer.getFamilyLinks().addAll(familyLinks);
            }
        }

        Customer updated = customerRepository.save(customer);
        return toResponseDto(updated);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> findAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> getCustomersPaginated(int page, int size,
                                                           String sortBy, String sortDirection) {
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
    @Transactional(readOnly = true)
    public CustomerDetailResponseDto findCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));
        return toDetailResponseDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> searchCustomerByNameOrNic(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new InvalidRequestException("Search keyword cannot be empty");
        }
        return customerRepository.searchByNameOrNic(keyword.trim())
                .stream()
                .map(this::toResponseDto)
                .toList();
    }


    @Override
    public void deleteCustomer(Long customerId) {
        customerRepository.findById(customerId)
                .ifPresentOrElse(
                        customerRepository::delete,
                        () -> { throw new ResourceNotFoundException(
                                "Customer with ID: " + customerId + " not found"); }
                );
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
                        .cityName(a.getCity() != null ? a.getCity().getName() : null)
                        .countryName(a.getCity() != null && a.getCity().getCountry() != null
                                ? a.getCity().getCountry().getName() : null)
                        .build())
                .toList();

        dto.setPhoneNumbers(phoneNumbers);
        dto.setAddresses(addresses);

        return dto;
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
                        .cityName(a.getCity() != null ? a.getCity().getName() : null)
                        .countryName(a.getCity() != null && a.getCity().getCountry() != null
                                ? a.getCity().getCountry().getName() : null)
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
}