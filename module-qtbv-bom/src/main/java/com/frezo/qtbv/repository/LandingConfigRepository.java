package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.LandingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LandingConfigRepository extends JpaRepository<LandingConfig, String> {
    Optional<LandingConfig> findByIsActiveTrue();
}
