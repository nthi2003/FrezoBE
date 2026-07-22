package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.DepreciationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepreciationScheduleRepository extends JpaRepository<DepreciationSchedule, String> {

    Optional<DepreciationSchedule> findByAssetIdAndIsDeletedFalse(String assetId);

    List<DepreciationSchedule> findByStatusAndIsDeletedFalse(String status);
}
