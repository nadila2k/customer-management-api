# Customer Management API

A Spring Boot-based REST API for managing customers, including bulk upload functionality, validation, and initial data seeding.

---

## Prerequisites

Before running the application, make sure you have installed:

* Java 8+
* Maven
* MariaDB

---

## Database Setup

1. Open your MariaDB client.
2. Create a new database:

```sql
CREATE DATABASE customer_db;
```

---

## Application Configuration

Update your `application.properties` file with the following database configuration:

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/customer_db
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Make sure the username and password match your local MariaDB setup.

---

## Running the Application

Use Maven to start the application:

```bash
mvn spring-boot:run
```

Or run the main class from your IDE.

---

## Initial Data Loading

When the application starts:

* Default City and Country data will be automatically initialized.
* Sample Customer records will also be inserted into the database.

This helps with testing and development without manual data entry.

---

## Technologies Used

Based on your `pom.xml`, the project uses:

* Spring Boot 2.7.18
* Spring Data JPA
* Spring Web
* Spring Validation
* MariaDB Java Client
* ModelMapper (3.2.0)
* Lombok
* Apache POI (5.2.3)
* Spring Boot Test (JUnit)

---

## Notes

* Ensure MariaDB service is running before starting the application.
* Default credentials (root/root) are used for development only.
* Hibernate ddl-auto=update will automatically create or update tables.
