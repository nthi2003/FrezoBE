package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, String> {

    List<Banner> findByIsDeletedFalseOrderByPositionAscOrderIndexAsc();

    Optional<Banner> findByIdAndIsDeletedFalse(String id);

    List<Banner> findByStatusAndIsDeletedFalseOrderByPositionAscOrderIndexAsc(String status);

    List<Banner> findByPinForNewsPageTrueAndStatusAndIsDeletedFalseOrderByOrderIndexAsc(
            String status);
}
