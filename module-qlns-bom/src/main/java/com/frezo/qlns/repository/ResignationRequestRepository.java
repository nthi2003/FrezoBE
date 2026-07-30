package com.frezo.qlns.repository;

import com.frezo.qlns.entity.ResignationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResignationRequestRepository extends JpaRepository<ResignationRequest, String> {

    List<ResignationRequest> findByIsDeletedFalseOrderByCreatedDateDesc();

    List<ResignationRequest> findByPersonIdAndIsDeletedFalseOrderByCreatedDateDesc(String personId);

    List<ResignationRequest> findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(String status);

    Optional<ResignationRequest> findByIdAndIsDeletedFalse(String id);
}
