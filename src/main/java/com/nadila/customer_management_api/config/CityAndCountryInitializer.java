package com.nadila.customer_management_api.config;

import com.nadila.customer_management_api.entity.City;
import com.nadila.customer_management_api.entity.Country;
import com.nadila.customer_management_api.repository.CityRepository;
import com.nadila.customer_management_api.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class CityAndCountryInitializer implements CommandLineRunner {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Override
    public void run(String... args) {

        if (countryRepository.count() > 0) {
            return;
        }


        Country sriLanka = createCountry("Sri Lanka");
        Country india = createCountry("India");
        Country usa = createCountry("USA");
        Country uk = createCountry("United Kingdom");

        List<Country> countries = List.of(sriLanka, india, usa, uk);
        countryRepository.saveAll(countries);


        saveCities(sriLanka, List.of(
                "Colombo", "Kandy", "Galle", "Jaffna",
                "Negombo", "Matara", "Kurunegala", "Anuradhapura","Gampaha"
        ));

        saveCities(india, List.of(
                "Delhi", "Mumbai", "Chennai", "Bangalore",
                "Kolkata", "Hyderabad", "Pune", "Jaipur"
        ));

        saveCities(usa, List.of(
                "New York", "Los Angeles", "Chicago", "Houston",
                "Phoenix", "Philadelphia", "San Antonio", "San Diego"
        ));

        saveCities(uk, List.of(
                "London", "Manchester", "Birmingham", "Liverpool",
                "Leeds", "Glasgow", "Bristol", "Oxford"
        ));
    }

    private Country createCountry(String name) {
        Country country = new Country();
        country.setName(name);
        return country;
    }

    private void saveCities(Country country, List<String> cityNames) {

        List<City> cities = new ArrayList<>();

        for (String name : cityNames) {
            City city = new City();
            city.setName(name);
            city.setCountry(country);
            cities.add(city);
        }

        cityRepository.saveAll(cities);
    }
}