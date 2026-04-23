package com.nadila.customer_management_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing

public class CustomerManagementApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerManagementApiApplication.class, args);
	}

}
