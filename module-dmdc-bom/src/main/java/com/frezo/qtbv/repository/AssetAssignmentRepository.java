package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, String> {

    /** Timeline mới → cũ cho drawer chi tiết. */
    List<AssetAssignment> findByAssetIdOrderByCreatedDateDesc(String assetId);

    /** Tất cả asset 1 người từng giữ (cho tab "Đang giữ" trên profile). */
    List<AssetAssignment> findByPersonIdOrderByCreatedDateDesc(String personId);
}
