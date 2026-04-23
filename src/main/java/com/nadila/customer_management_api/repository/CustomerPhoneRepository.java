package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.CustomerPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerPhoneRepository extends JpaRepository<CustomerPhone, Long> {

    void deleteByCustomerId(Long customerId);
}
