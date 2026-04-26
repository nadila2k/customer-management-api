package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.CustomerFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerFamilyRepository extends JpaRepository<CustomerFamily, Long> {
    Optional<CustomerFamily> findByCustomerIdAndFamilyMemberId(
            Long customerId, Long familyMemberId);
}
