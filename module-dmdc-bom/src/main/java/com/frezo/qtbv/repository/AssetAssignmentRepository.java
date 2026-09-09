package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, String> {

    List<AssetAssignment> findByAssetIdOrderByCreatedDateDesc(String assetId);
}
