package com.example.bankingprojectfinal.Model.Entity;


import com.example.bankingprojectfinal.Model.Enums.CustomerStatus;
import com.example.bankingprojectfinal.security.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
@Builder
@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(unique = true, nullable = false)
    private String finCode;
    @Column(unique = true, nullable = false)
    private String phoneNumber;



    private LocalDate registrationDate;

    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountEntity> accountList;
}