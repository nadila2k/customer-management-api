package com.nadila.customer_management_api.service.customerService;

import com.nadila.customer_management_api.dto.requestDto.AddressRequestDto;
import com.nadila.customer_management_api.dto.requestDto.CustomerRequestDto;
import com.nadila.customer_management_api.dto.requestDto.PhoneRequestDto;
import com.nadila.customer_management_api.dto.responseDto.*;
import com.nadila.customer_management_api.entity.*;
import com.nadila.customer_management_api.exception.*;
import com.nadila.customer_management_api.repository.CustomerRepository;
import com.nadila.customer_management_api.service.cityService.CityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CityService cityService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;



    private CustomerRequestDto baseRequest;
    private Customer baseCustomer;
    private City colombo;

    @BeforeEach
    void setUp() {
        colombo = new City();
        colombo.setId(1L);
        colombo.setName("Colombo");
        Country sriLanka = new Country();
        sriLanka.setName("Sri Lanka");
        colombo.setCountry(sriLanka);

        baseRequest = CustomerRequestDto.builder()
                .name("Nadila Perera")
                .dateOfBirth(LocalDate.of(1995, 6, 15))
                .nicNumber("950123456V")
                .build();

        baseCustomer = new Customer();
        baseCustomer.setId(1L);
        baseCustomer.setName("Nadila Perera");
        baseCustomer.setDateOfBirth(LocalDate.of(1995, 6, 15));
        baseCustomer.setNicNumber("950123456V");
        baseCustomer.setCreatedAt(LocalDateTime.now());
        baseCustomer.setUpdatedAt(LocalDateTime.now());
    }




    private void stubMapperForCreate() {
        when(modelMapper.map(any(CustomerRequestDto.class), eq(Customer.class)))
                .thenReturn(baseCustomer);
    }

    private void stubResponseMapper() {
        when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                .thenReturn(buildBasicResponseDto());
    }

    private CustomerResponseDto buildBasicResponseDto() {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(1L);
        dto.setName("Nadila Perera");
        dto.setNicNumber("950123456V");
        dto.setPhoneNumbers(List.of());
        dto.setAddresses(List.of());
        return dto;
    }



    @Nested
    @DisplayName("createCustomer()")
    class CreateCustomer {

        @Test
        @DisplayName("creates customer successfully with minimal fields")
        void createCustomer_minimal_success() {
            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            CustomerResponseDto result = customerService.createCustomer(baseRequest);

            assertThat(result).isNotNull();
            assertThat(result.getNicNumber()).isEqualTo("950123456V");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("creates customer with phones and addresses")
        void createCustomer_withPhonesAndAddresses_success() {
            baseRequest.setPhones(List.of(
                    PhoneRequestDto.builder().mobileNumber("+94771234567").build()
            ));
            baseRequest.setAddresses(List.of(
                    AddressRequestDto.builder()
                            .addressLine1("No 5, Main Street")
                            .cityName("Colombo")
                            .build()
            ));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(cityService.getCityByName("Colombo")).thenReturn(colombo);
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            CustomerResponseDto result = customerService.createCustomer(baseRequest);

            assertThat(result).isNotNull();
            verify(cityService).getCityByName("Colombo");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("creates customer with valid family member IDs")
        void createCustomer_withFamilyMembers_success() {
            Customer familyMember = new Customer();
            familyMember.setId(2L);

            baseRequest.setFamilyMemberIds(List.of(2L));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(customerRepository.findById(2L)).thenReturn(Optional.of(familyMember));
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            CustomerResponseDto result = customerService.createCustomer(baseRequest);

            assertThat(result).isNotNull();
            verify(customerRepository).findById(2L);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when NIC already exists")
        void createCustomer_duplicateNic_throws() {
            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(baseRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("950123456V");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws DuplicateFamilyMemberException when duplicate family IDs provided")
        void createCustomer_duplicateFamilyIds_throws() {
            baseRequest.setFamilyMemberIds(List.of(2L, 2L));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();

            assertThatThrownBy(() -> customerService.createCustomer(baseRequest))
                    .isInstanceOf(DuplicateFamilyMemberException.class)
                    .hasMessageContaining("Duplicate family member IDs");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when family member ID does not exist")
        void createCustomer_familyMemberNotFound_throws() {
            baseRequest.setFamilyMemberIds(List.of(99L));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.createCustomer(baseRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("creates customer with 12-digit NIC")
        void createCustomer_twelveDigitNic_success() {
            baseRequest.setNicNumber("199506123456");
            baseCustomer.setNicNumber("199506123456");

            when(customerRepository.existsByNicNumber("199506123456")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            CustomerResponseDto result = customerService.createCustomer(baseRequest);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("creates customer with multiple phones")
        void createCustomer_multiplePhones_success() {
            baseRequest.setPhones(List.of(
                    PhoneRequestDto.builder().mobileNumber("+94771234567").build(),
                    PhoneRequestDto.builder().mobileNumber("+94711234567").build()
            ));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            customerService.createCustomer(baseRequest);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getPhones()).hasSize(2);
        }

        @Test
        @DisplayName("creates customer with address in Kandy (Sri Lanka)")
        void createCustomer_addressInKandy_success() {
            City kandy = new City();
            kandy.setName("Kandy");
            Country sriLanka = new Country();
            sriLanka.setName("Sri Lanka");
            kandy.setCountry(sriLanka);

            baseRequest.setAddresses(List.of(
                    AddressRequestDto.builder()
                            .addressLine1("100 Kandy Road")
                            .cityName("Kandy")
                            .build()
            ));

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            stubMapperForCreate();
            stubResponseMapper();
            when(cityService.getCityByName("Kandy")).thenReturn(kandy);
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            customerService.createCustomer(baseRequest);

            verify(cityService).getCityByName("Kandy");
        }
    }

    @Nested
    @DisplayName("updateCustomer()")
    class UpdateCustomer {

        @Test
        @DisplayName("updates customer name and NIC successfully")
        void updateCustomer_basicFields_success() {
            baseCustomer.setPhones(new ArrayList<>());
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());

            CustomerRequestDto updateReq = CustomerRequestDto.builder()
                    .name("Nadila Silva")
                    .dateOfBirth(LocalDate.of(1995, 6, 15))
                    .nicNumber("950123456V")
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));
            when(customerRepository.save(any())).thenReturn(baseCustomer);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            CustomerResponseDto result = customerService.updateCustomer(1L, updateReq);

            assertThat(result).isNotNull();
            verify(customerRepository).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when customer not found")
        void updateCustomer_customerNotFound_throws() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, baseRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("throws DuplicateResourceException when new NIC belongs to another customer")
        void updateCustomer_nicTakenByAnother_throws() {
            baseCustomer.setNicNumber("950123456V");

            CustomerRequestDto updateReq = CustomerRequestDto.builder()
                    .name("Nadila")
                    .dateOfBirth(LocalDate.of(1995, 6, 15))
                    .nicNumber("199506123456") // different NIC
                    .build();

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));
            when(customerRepository.existsByNicNumber("199506123456")).thenReturn(true);

            assertThatThrownBy(() -> customerService.updateCustomer(1L, updateReq))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("199506123456");
        }

        @Test
        @DisplayName("allows update with same NIC (no duplicate check triggered)")
        void updateCustomer_sameNic_success() {
            baseCustomer.setPhones(new ArrayList<>());
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());
            baseCustomer.setNicNumber("950123456V");

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));
            when(customerRepository.save(any())).thenReturn(baseCustomer);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            customerService.updateCustomer(1L, baseRequest);

            verify(customerRepository, never()).existsByNicNumber(any());
        }

        @Test
        @DisplayName("throws DuplicateFamilyMemberException on duplicate family IDs during update")
        void updateCustomer_duplicateFamilyIds_throws() {
            baseCustomer.setPhones(new ArrayList<>());
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());

            baseRequest.setFamilyMemberIds(List.of(2L, 2L));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            assertThatThrownBy(() -> customerService.updateCustomer(1L, baseRequest))
                    .isInstanceOf(DuplicateFamilyMemberException.class);
        }

        @Test
        @DisplayName("throws SelfFamilyReferenceException when customer adds themselves as family")
        void updateCustomer_selfFamilyReference_throws() {
            baseCustomer.setPhones(new ArrayList<>());
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());

            baseRequest.setFamilyMemberIds(List.of(1L)); // same as customerId

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            assertThatThrownBy(() -> customerService.updateCustomer(1L, baseRequest))
                    .isInstanceOf(SelfFamilyReferenceException.class);
        }

        @Test
        @DisplayName("replaces existing phones with new ones on update")
        void updateCustomer_replacesPhones() {
            CustomerPhone existingPhone = CustomerPhone.builder()
                    .id(10L).mobileNumber("+94771111111").customer(baseCustomer).build();
            baseCustomer.setPhones(new ArrayList<>(List.of(existingPhone)));
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());

            baseRequest.setPhones(List.of(
                    PhoneRequestDto.builder().mobileNumber("+94779999999").build()
            ));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));
            when(customerRepository.save(any())).thenReturn(baseCustomer);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            customerService.updateCustomer(1L, baseRequest);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getPhones())
                    .extracting(CustomerPhone::getMobileNumber)
                    .containsExactly("+94779999999");
        }

        @Test
        @DisplayName("updates address with Mumbai city (India)")
        void updateCustomer_addressInMumbai_success() {
            City mumbai = new City();
            mumbai.setName("Mumbai");
            Country india = new Country();
            india.setName("India");
            mumbai.setCountry(india);

            baseCustomer.setPhones(new ArrayList<>());
            baseCustomer.setAddresses(new ArrayList<>());
            baseCustomer.setFamilyLinks(new ArrayList<>());

            baseRequest.setAddresses(List.of(
                    AddressRequestDto.builder()
                            .addressLine1("10 Marine Lines")
                            .cityName("Mumbai")
                            .build()
            ));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));
            when(cityService.getCityByName("Mumbai")).thenReturn(mumbai);
            when(customerRepository.save(any())).thenReturn(baseCustomer);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            customerService.updateCustomer(1L, baseRequest);

            verify(cityService).getCityByName("Mumbai");
        }
    }



    @Nested
    @DisplayName("findAllCustomers()")
    class FindAllCustomers {

        @Test
        @DisplayName("returns empty list when no customers exist")
        void findAll_empty() {
            when(customerRepository.findAll()).thenReturn(List.of());

            List<CustomerResponseDto> result = customerService.findAllCustomers();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns mapped list of all customers")
        void findAll_returnsMappedList() {
            Customer c2 = new Customer();
            c2.setId(2L);
            c2.setName("Amara Silva");
            c2.setNicNumber("199506123456");

            when(customerRepository.findAll()).thenReturn(List.of(baseCustomer, c2));
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            List<CustomerResponseDto> result = customerService.findAllCustomers();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns single customer when only one exists")
        void findAll_singleCustomer() {
            when(customerRepository.findAll()).thenReturn(List.of(baseCustomer));
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            List<CustomerResponseDto> result = customerService.findAllCustomers();

            assertThat(result).hasSize(1);
        }
    }



    @Nested
    @DisplayName("getCustomersPaginated()")
    class GetCustomersPaginated {

        @Test
        @DisplayName("returns paginated result sorted by name ascending")
        void paginated_byName_asc() {
            Page<Customer> page = new PageImpl<>(List.of(baseCustomer));
            when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            Page<CustomerResponseDto> result =
                    customerService.getCustomersPaginated(0, 10, "name", "asc");

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("returns paginated result sorted by dateOfBirth descending")
        void paginated_byDateOfBirth_desc() {
            Page<Customer> page = new PageImpl<>(List.of(baseCustomer));
            when(customerRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            Page<CustomerResponseDto> result =
                    customerService.getCustomersPaginated(0, 5, "dateOfBirth", "desc");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws InvalidRequestException for invalid sort field")
        void paginated_invalidSortField_throws() {
            assertThatThrownBy(() ->
                    customerService.getCustomersPaginated(0, 10, "invalidField", "asc"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("invalidField");
        }

        @Test
        @DisplayName("allows all valid sort fields without throwing")
        void paginated_allValidSortFields_noThrow() {
            Page<Customer> emptyPage = new PageImpl<>(List.of());
            when(customerRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            List<String> validFields = List.of("name", "dateOfBirth", "nicNumber", "createdAt", "updatedAt");
            for (String field : validFields) {
                assertThatNoException().isThrownBy(() ->
                        customerService.getCustomersPaginated(0, 10, field, "asc"));
            }
        }

        @Test
        @DisplayName("returns empty page when no customers match")
        void paginated_emptyPage() {
            when(customerRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<CustomerResponseDto> result =
                    customerService.getCustomersPaginated(0, 10, "name", "asc");

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }



    @Nested
    @DisplayName("findCustomerById()")
    class FindCustomerById {

        @Test
        @DisplayName("returns detail response for existing customer")
        void findById_found() {
            baseCustomer.setPhones(List.of());
            baseCustomer.setAddresses(List.of());
            baseCustomer.setFamilyLinks(List.of());

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            CustomerDetailResponseDto result = customerService.findCustomerById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Nadila Perera");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for non-existent ID")
        void findById_notFound_throws() {
            when(customerRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findCustomerById(404L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("404");
        }

        @Test
        @DisplayName("returns family members in detail response")
        void findById_includesFamilyMembers() {
            Customer member = new Customer();
            member.setId(2L);
            member.setName("Amara Silva");
            member.setNicNumber("199506123456");

            CustomerFamily link = CustomerFamily.builder()
                    .customer(baseCustomer)
                    .familyMember(member)
                    .build();

            baseCustomer.setPhones(List.of());
            baseCustomer.setAddresses(List.of());
            baseCustomer.setFamilyLinks(List.of(link));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            CustomerDetailResponseDto result = customerService.findCustomerById(1L);

            assertThat(result.getFamilyMembers()).hasSize(1);
            assertThat(result.getFamilyMembers().get(0).getName()).isEqualTo("Amara Silva");
        }

        @Test
        @DisplayName("returns address with city and country in detail response")
        void findById_includesAddressWithCityCountry() {
            CustomerAddress address = CustomerAddress.builder()
                    .id(1L)
                    .customer(baseCustomer)
                    .addressLine1("No 5, Galle Road")
                    .city(colombo)
                    .build();

            baseCustomer.setPhones(List.of());
            baseCustomer.setAddresses(List.of(address));
            baseCustomer.setFamilyLinks(List.of());

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            CustomerDetailResponseDto result = customerService.findCustomerById(1L);

            assertThat(result.getAddresses()).hasSize(1);
            assertThat(result.getAddresses().get(0).getCityName()).isEqualTo("Colombo");
            assertThat(result.getAddresses().get(0).getCountryName()).isEqualTo("Sri Lanka");
        }
    }

    @Nested
    @DisplayName("searchCustomerByNameOrNic()")
    class SearchCustomerByNameOrNic {

        @Test
        @DisplayName("returns matching customers for valid keyword")
        void search_validKeyword_returnsResults() {
            when(customerRepository.searchByNameOrNic("Nadila")).thenReturn(List.of(baseCustomer));
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            List<CustomerResponseDto> result =
                    customerService.searchCustomerByNameOrNic("Nadila");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no customer matches keyword")
        void search_noMatch_returnsEmpty() {
            when(customerRepository.searchByNameOrNic("unknown")).thenReturn(List.of());

            List<CustomerResponseDto> result =
                    customerService.searchCustomerByNameOrNic("unknown");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("throws InvalidRequestException for blank keyword")
        void search_blankKeyword_throws() {
            assertThatThrownBy(() -> customerService.searchCustomerByNameOrNic("   "))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("throws InvalidRequestException for null keyword")
        void search_nullKeyword_throws() {
            assertThatThrownBy(() -> customerService.searchCustomerByNameOrNic(null))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("searches by NIC number keyword")
        void search_byNicKeyword_returnsResults() {
            when(customerRepository.searchByNameOrNic("950123456V")).thenReturn(List.of(baseCustomer));
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            List<CustomerResponseDto> result =
                    customerService.searchCustomerByNameOrNic("950123456V");

            assertThat(result).hasSize(1);
            verify(customerRepository).searchByNameOrNic("950123456V");
        }

        @Test
        @DisplayName("trims whitespace from keyword before searching")
        void search_trimsKeyword() {
            when(customerRepository.searchByNameOrNic("Nadila")).thenReturn(List.of(baseCustomer));
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class)))
                    .thenReturn(buildBasicResponseDto());

            customerService.searchCustomerByNameOrNic("  Nadila  ");

            verify(customerRepository).searchByNameOrNic("Nadila");
        }
    }


    @Nested
    @DisplayName("deleteCustomer()")
    class DeleteCustomer {

        @Test
        @DisplayName("deletes existing customer successfully")
        void delete_existingCustomer_success() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            customerService.deleteCustomer(1L);

            verify(customerRepository).delete(baseCustomer);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when customer not found")
        void delete_notFound_throws() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(customerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("does not call delete when customer is absent")
        void delete_absent_neverCallsDelete() {
            when(customerRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deleteCustomer(5L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(customerRepository, never()).delete(any(Customer.class));
        }
    }


    @Nested
    @DisplayName("toResponseDto() mapping")
    class ToResponseDtoMapping {

        @Test
        @DisplayName("maps phone numbers correctly in response")
        void responseDto_phoneMapped() {
            CustomerPhone phone = CustomerPhone.builder()
                    .id(10L).mobileNumber("+94771234567").customer(baseCustomer).build();
            baseCustomer.setPhones(List.of(phone));
            baseCustomer.setAddresses(List.of());
            baseCustomer.setFamilyLinks(null);

            when(customerRepository.existsByNicNumber("950123456V")).thenReturn(false);
            when(modelMapper.map(any(CustomerRequestDto.class), eq(Customer.class)))
                    .thenReturn(baseCustomer);
            when(customerRepository.save(any())).thenReturn(baseCustomer);

            CustomerResponseDto dto = new CustomerResponseDto();
            dto.setId(1L);
            dto.setName("Nadila Perera");
            dto.setNicNumber("950123456V");
            when(modelMapper.map(any(Customer.class), eq(CustomerResponseDto.class))).thenReturn(dto);

            CustomerResponseDto result = customerService.createCustomer(baseRequest);

            assertThat(result.getPhoneNumbers())
                    .extracting(PhoneResponseDto::getMobileNumber)
                    .containsExactly("+94771234567");
        }

        @Test
        @DisplayName("maps address with null city gracefully")
        void responseDto_nullCity_handledGracefully() {
            CustomerAddress address = CustomerAddress.builder()
                    .id(1L)
                    .addressLine1("Some address")
                    .city(null)
                    .customer(baseCustomer)
                    .build();
            baseCustomer.setPhones(List.of());
            baseCustomer.setAddresses(List.of(address));
            baseCustomer.setFamilyLinks(List.of());

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            CustomerDetailResponseDto result = customerService.findCustomerById(1L);

            assertThat(result.getAddresses().get(0).getCityName()).isNull();
            assertThat(result.getAddresses().get(0).getCountryName()).isNull();
        }

        @Test
        @DisplayName("maps address with city but null country gracefully")
        void responseDto_cityWithNullCountry_handledGracefully() {
            City cityNoCountry = new City();
            cityNoCountry.setName("Gampaha");
            cityNoCountry.setCountry(null);

            CustomerAddress address = CustomerAddress.builder()
                    .id(2L)
                    .addressLine1("456 Kandy Road")
                    .city(cityNoCountry)
                    .customer(baseCustomer)
                    .build();

            baseCustomer.setPhones(List.of());
            baseCustomer.setAddresses(List.of(address));
            baseCustomer.setFamilyLinks(List.of());

            when(customerRepository.findById(1L)).thenReturn(Optional.of(baseCustomer));

            CustomerDetailResponseDto result = customerService.findCustomerById(1L);

            assertThat(result.getAddresses().get(0).getCityName()).isEqualTo("Gampaha");
            assertThat(result.getAddresses().get(0).getCountryName()).isNull();
        }
    }
}