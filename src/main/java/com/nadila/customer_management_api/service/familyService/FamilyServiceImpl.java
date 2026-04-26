package com.nadila.customer_management_api.service.familyService;

import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.CustomerFamilyRepository;
import com.nadila.customer_management_api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyServiceImpl implements FamilyService {

    private final CustomerFamilyRepository customerFamilyRepository;
    private final CustomerRepository customerRepository;

    @Override
    public void deleteFamilyMember(Long customerId, Long familyMemberId) {


        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));


        customerFamilyRepository
                .findByCustomerIdAndFamilyMemberId(customerId, familyMemberId)
                .ifPresentOrElse(
                        customerFamilyRepository::delete,
                        () -> { throw new ResourceNotFoundException(
                                "Family member link not found for customer: "
                                        + customerId + " and member: " + familyMemberId); }
                );
    }
}
