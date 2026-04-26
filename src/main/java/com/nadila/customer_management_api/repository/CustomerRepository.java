package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNicNumber(String nicNumber);

    Optional<Object> findByNicNumber(String nicNumber);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.nicNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Customer> searchByNameOrNic(@Param("keyword") String keyword);
}
