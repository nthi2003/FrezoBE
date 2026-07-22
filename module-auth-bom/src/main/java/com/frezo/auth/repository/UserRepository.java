package com.frezo.auth.repository;

import com.frezo.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByUserName(String userName);

    /**
     * Resolve username từ personId — dùng bởi Ticket/Payroll/Leave notifications
     * (nơi entity chỉ giữ personId nhưng notification cần username).
     */
    Optional<User> findByPersonId(String personId);
}
