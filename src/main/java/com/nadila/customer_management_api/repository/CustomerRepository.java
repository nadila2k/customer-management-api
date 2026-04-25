package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNicNumber(String nicNumber);

    Optional<Object> findByNicNumber(String nicNumber);
}
