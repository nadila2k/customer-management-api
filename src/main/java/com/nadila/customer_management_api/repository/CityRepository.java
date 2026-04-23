package com.nadila.customer_management_api.repository;

import com.nadila.customer_management_api.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<Object> findByNameIgnoreCase(@NotBlank(message = "City name is required") @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters") String cityName);


    City findByName(String colombo);
}
