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
        if (customerRepository.count() > 0) return; // ✅ skip if already initialized

        // ✅ Sri Lanka customers
        createCustomer("Nadila Perera",    "2000-03-15", "NIC001LK", "+94771234561", "123 Main St", null,       "Colombo");
        createCustomer("Kasun Silva",      "1995-07-22", "NIC002LK", "+94771234562", "45 Lake Rd",  "Floor 2",  "Kandy");
        createCustomer("Dilani Fernando",  "1988-11-10", "NIC003LK", "+94771234563", "78 Beach Ave", null,      "Galle");
        createCustomer("Ruwan Jayasinghe", "1992-04-18", "NIC004LK", "+94771234564", "12 Fort Rd",  null,       "Jaffna");
        createCustomer("Amaya Dissanayake","2001-09-05", "NIC005LK", "+94771234565", "56 Sea St",   "Apt 3",    "Negombo");
        createCustomer("Tharaka Rathnayake","1990-01-30","NIC006LK", "+94771234566", "90 Temple Rd",null,       "Matara");
        createCustomer("Priya Wijesinghe", "1997-06-14", "NIC007LK", "+94771234567", "34 Hill St",  null,       "Kurunegala");
        createCustomer("Chamara Bandara",  "1985-12-25", "NIC008LK", "+94771234568", "67 Sacred Rd",null,       "Anuradhapura");

        // ✅ India customers
        createCustomer("Arjun Sharma",     "1993-02-11", "NIC001IN", "+91991234561", "22 India Gate","Block A",  "Delhi");
        createCustomer("Priya Patel",      "1998-08-19", "NIC002IN", "+91991234562", "88 Marine Dr", null,       "Mumbai");
        createCustomer("Raj Kumar",        "1987-05-07", "NIC003IN", "+91991234563", "15 Beach Rd",  null,       "Chennai");
        createCustomer("Sneha Reddy",      "2000-10-23", "NIC004IN", "+91991234564", "42 MG Road",   "Suite 5",  "Bangalore");
        createCustomer("Amit Das",         "1994-03-16", "NIC005IN", "+91991234565", "7 Park St",    null,       "Kolkata");
        createCustomer("Kavitha Rao",      "1991-07-29", "NIC006IN", "+91991234566", "19 Hi-Tech City",null,     "Hyderabad");
        createCustomer("Rohit Desai",      "1996-11-04", "NIC007IN", "+91991234567", "55 FC Road",   "Apt 8",    "Pune");
        createCustomer("Anita Singh",      "1989-01-17", "NIC008IN", "+91991234568", "30 Pink City",  null,      "Jaipur");

        // ✅ USA customers
        createCustomer("James Wilson",     "1990-04-12", "NIC001US", "+12121234561", "500 5th Ave",  "Floor 10", "New York");
        createCustomer("Emily Johnson",    "1995-09-28", "NIC002US", "+13101234562", "200 Sunset Blvd",null,     "Los Angeles");
        createCustomer("Michael Brown",    "1983-06-03", "NIC003US", "+13121234563", "100 Michigan Ave",null,    "Chicago");
        createCustomer("Sarah Davis",      "1999-12-15", "NIC004US", "+17131234564", "300 Main St",  "Apt 2",    "Houston");
        createCustomer("David Martinez",   "1992-02-20", "NIC005US", "+16021234565", "75 Desert Rd", null,       "Phoenix");
        createCustomer("Jessica Garcia",   "1988-08-08", "NIC006US", "+12151234566", "400 Liberty Ave",null,     "Philadelphia");
        createCustomer("Chris Lee",        "1997-05-31", "NIC007US", "+12101234567", "25 Alamo Plaza",null,      "San Antonio");
        createCustomer("Ashley Taylor",    "2001-11-22", "NIC008US", "+16191234568", "10 Harbor Dr", "Unit 4",   "San Diego");

        // ✅ UK customers
        createCustomer("Oliver Smith",     "1991-03-09", "NIC001UK", "+44201234561", "10 Downing St","Flat 3",   "London");
        createCustomer("Charlotte Jones",  "1996-07-14", "NIC002UK", "+44611234562", "5 Piccadilly",  null,      "Manchester");
        createCustomer("Harry Williams",   "1984-10-27", "NIC003UK", "+44121234563", "22 Bull Ring",  null,      "Birmingham");
        createCustomer("Sophia Brown",     "2000-01-05", "NIC004UK", "+44511234564", "8 Mathew St",  "Floor 2",  "Liverpool");
        createCustomer("George Taylor",    "1993-04-19", "NIC005UK", "+44131234565", "15 The Headrow",null,      "Leeds");
        createCustomer("Isla Anderson",    "1987-09-30", "NIC006UK", "+44141234566", "33 Buchanan St",null,      "Glasgow");
        createCustomer("Liam Thomas",      "1998-06-11", "NIC007UK", "+44117234567", "9 Park St",    "Apt 1",    "Bristol");
        createCustomer("Emma White",       "1994-12-03", "NIC008UK", "+44186234568", "4 High St",    null,       "Oxford");

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
                .addressLine2(line2)  // ✅ null is fine for optional line2
                .city(city)
                .build();

        customer.setPhones(List.of(customerPhone));
        customer.setAddresses(List.of(customerAddress));
        // ✅ familyLinks not set — null is valid, customer may have no family members

        customerRepository.save(customer);
    }
}
