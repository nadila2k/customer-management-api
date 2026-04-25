package com.nadila.customer_management_api.service.cityService;

import com.nadila.customer_management_api.entity.City;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

public interface CityService {
    City getCityByName(String cityName);
    List<City> getAllCities();
    void warmUpCityCache();

    @CacheEvict(value = "cities", allEntries = true)
    void evictCityCache();
}

