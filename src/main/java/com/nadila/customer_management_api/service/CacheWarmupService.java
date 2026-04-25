package com.nadila.customer_management_api.service;

import com.nadila.customer_management_api.service.cityService.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Order(2)  // ✅ runs after DataInitializer (Order 1)
@Slf4j
public class CacheWarmupService implements CommandLineRunner {

    private final CityService cityService;

    @Override
    public void run(String... args) {
        log.info("🔥 Starting cache warm-up...");
        cityService.warmUpCityCache();
        log.info("✅ Cache warm-up complete!");
    }
}