package com.nadila.customer_management_api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "nic_number", nullable = false, unique = true, length = 20)
    private String nicNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_customer_id")
    private Customer primaryCustomer;

    @OneToMany(mappedBy = "primaryCustomer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Customer> familyMembers;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CustomerPhone> phones;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CustomerAddress> addresses;
}