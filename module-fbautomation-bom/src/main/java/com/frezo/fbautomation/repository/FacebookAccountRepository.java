package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.FacebookAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacebookAccountRepository
        extends JpaRepository<FacebookAccount, String>, JpaSpecificationExecutor<FacebookAccount> {

    Optional<FacebookAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<FacebookAccount> findByStatus(String status);

    List<FacebookAccount> findByStatusAndPostsTodayLessThan(String status, Integer maxPosts);
}
