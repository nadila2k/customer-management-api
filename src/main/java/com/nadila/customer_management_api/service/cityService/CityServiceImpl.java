package com.nadila.customer_management_api.service.cityService;

import com.nadila.customer_management_api.entity.City;
import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CacheManager cacheManager;

    @Cacheable(value = "cities", key = "#cityName.toLowerCase()")
    @Override
    public City getCityByName(String cityName) {
        log.info("🔍 DB hit for city: {}", cityName);
        return (City) cityRepository.findByNameIgnoreCase(cityName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "City not found: " + cityName));
    }

    @Cacheable(value = "cities", key = "'all'")
    @Override
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }


    @Override
    public void warmUpCityCache() {
        List<City> cities = cityRepository.findAll();

        Cache cache = cacheManager.getCache("cities");

        if (cache != null) {
            cities.forEach(city -> {
                cache.put(city.getName().toLowerCase(), city);
            });
            log.info("✅ {} cities loaded into cache", cities.size());
        } else {
            log.warn("⚠️ Cache 'cities' not found!");
        }
    }

    @CacheEvict(value = "cities", allEntries = true)
    @Override
    public void evictCityCache() {
        log.info("🗑️ City cache cleared");
    }
}