package com.nadila.customer_management_api.config;

import com.nadila.customer_management_api.entity.City;
import com.nadila.customer_management_api.entity.Customer;
import com.nadila.customer_management_api.entity.CustomerAddress;
import com.nadila.customer_management_api.entity.CustomerPhone;
import com.nadila.customer_management_api.exception.ResourceNotFoundException;
import com.nadila.customer_management_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class CustomerDataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final CityRepository cityRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) return;

        createCustomer("Nadila Perera",    "2000-03-15", "200007500123", "+94771234561", "123 Main St", null,       "Colombo");
        createCustomer("Kasun Silva",      "1995-07-22", "952031234V",   "+94771234562", "45 Lake Rd",  "Floor 2",  "Kandy");
        createCustomer("Dilani Fernando",  "1988-11-10", "888150123V",   "+94771234563", "78 Beach Ave", null,      "Galle");
        createCustomer("Ruwan Jayasinghe", "1992-04-18", "922091234V",   "+94771234564", "12 Fort Rd",  null,       "Jaffna");
        createCustomer("Amaya Dissanayake","2001-09-05", "200124500456", "+94771234565", "56 Sea St",   "Apt 3",    "Negombo");
        createCustomer("Tharaka Rathnayake","1990-01-30","900301234V",   "+94771234566", "90 Temple Rd",null,       "Matara");
        createCustomer("Priya Wijesinghe", "1997-06-14", "972151234V",   "+94771234567", "34 Hill St",  null,       "Kurunegala");
        createCustomer("Chamara Bandara",  "1985-12-25", "853591234V",   "+94771234568", "67 Sacred Rd",null,       "Anuradhapura");

        createCustomer("Arjun Sharma",     "1993-02-11", "932421234V",   "+94771234569", "22 Temple Rd","Block A",  "Colombo");
        createCustomer("Priya Patel",      "1998-08-19", "982311234V",   "+94771234570", "88 Lake Rd",  null,       "Kandy");
        createCustomer("Raj Kumar",        "1987-05-07", "871271234V",   "+94771234571", "15 Hill St",  null,       "Galle");
        createCustomer("Sneha Reddy",      "2000-10-23", "200029600789", "+94771234572", "42 Main St",  "Suite 5",  "Colombo");

        createCustomer("James Wilson",     "1990-04-12", "901021234V",   "+94771234573", "500 Street",  "Floor 10", "Negombo");
        createCustomer("Emily Johnson",    "1995-09-28", "952711234V",   "+94771234574", "200 Road",    null,       "Gampaha");
        createCustomer("Michael Brown",    "1983-06-03", "831541234V",   "+94771234575", "100 Ave",     null,       "Matara");
        createCustomer("Sarah Davis",      "1999-12-15", "993491234V",   "+94771234576", "300 Lane",    "Apt 2",    "Kandy");

        createCustomer("David Martinez",   "1992-02-20", "920511234V",   "+94771234577", "75 Road",     null,       "Colombo");
        createCustomer("Jessica Garcia",   "1988-08-08", "882211234V",   "+94771234578", "400 Street",  null,       "Galle");
        createCustomer("Chris Lee",        "1997-05-31", "971511234V",   "+94771234579", "25 Lane",     null,       "Jaffna");
        createCustomer("Ashley Taylor",    "2001-11-22", "200132600999", "+94771234580", "10 Road",     "Unit 4",   "Kurunegala");

        createCustomer("Oliver Smith",     "1991-03-09", "910681234V",   "+94771234581", "10 Street",   "Flat 3",   "Colombo");
        createCustomer("Charlotte Jones",  "1996-07-14", "961961234V",   "+94771234582", "5 Road",      null,       "Kandy");
        createCustomer("Harry Williams",   "1984-10-27", "843011234V",   "+94771234583", "22 Ave",      null,       "Galle");
        createCustomer("Sophia Brown",     "2000-01-05", "200000500321", "+94771234584", "8 Street",    "Floor 2",  "Matara");

        createCustomer("George Taylor",    "1993-04-19", "931091234V",   "+94771234585", "15 Road",     null,       "Colombo");
        createCustomer("Isla Anderson",    "1987-09-30", "872731234V",   "+94771234586", "33 Street",   null,       "Kandy");
        createCustomer("Liam Thomas",      "1998-06-11", "981621234V",   "+94771234587", "9 Lane",      "Apt 1",    "Galle");
        createCustomer("Emma White",       "1994-12-03", "943381234V",   "+94771234588", "4 Street",    null,       "Negombo");

        System.out.println("✅ Customer data initialized successfully!");
    }

    private void createCustomer(String name, String dob, String nic,
                                String phone, String line1, String line2, String cityName) {

        City city = (City) cityRepository.findByNameIgnoreCase(cityName)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityName));

        Customer customer = Customer.builder()
                .name(name)
                .dateOfBirth(LocalDate.parse(dob))
                .nicNumber(nic)
                .build();

        CustomerPhone customerPhone = CustomerPhone.builder()
                .customer(customer)
                .mobileNumber(phone)
                .build();

        CustomerAddress customerAddress = CustomerAddress.builder()
                .customer(customer)
                .addressLine1(line1)
                .addressLine2(line2)
                .city(city)
                .build();

        customer.setPhones(List.of(customerPhone));
        customer.setAddresses(List.of(customerAddress));


        customerRepository.save(customer);
    }
}
