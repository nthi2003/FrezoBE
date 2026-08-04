package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.PageReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageReviewRepository extends JpaRepository<PageReview, String> {
    List<PageReview> findByStatus(String status);
    long countByStatus(String status);
    long countByRatingLessThanEqual(Integer rating);
}
