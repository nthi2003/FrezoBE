package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, String> {

    List<Offer> findByApplicationIdAndIsDeletedFalseOrderByCreatedDateDesc(String applicationId);
}
