package com.frezo.customer.repository;

import com.frezo.customer.entity.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, String>,
        JpaSpecificationExecutor<CustomerPayment> {
    List<CustomerPayment> findByCustomerIdOrderByCreatedDateDesc(String customerId);
}
