package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    void deleteByCustomerId(Long customerId);
}
