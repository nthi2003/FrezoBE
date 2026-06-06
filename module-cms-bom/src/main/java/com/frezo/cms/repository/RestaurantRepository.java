package com.frezo.cms.repository;

import com.frezo.cms.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
    Optional<Restaurant> findByCode(String code);
}
