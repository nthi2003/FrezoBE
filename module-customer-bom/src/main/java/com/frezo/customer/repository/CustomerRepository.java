package com.frezo.customer.repository;

import com.frezo.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>,
        JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCode(String code);

    boolean existsByCode(String code);

    // search by phone hash (SHA-256)
    Optional<Customer> findByPhoneHash(String phoneHash);
}
