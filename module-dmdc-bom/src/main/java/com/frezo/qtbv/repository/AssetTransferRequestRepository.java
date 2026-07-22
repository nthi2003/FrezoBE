package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.AssetTransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetTransferRequestRepository
        extends JpaRepository<AssetTransferRequest, String>, JpaSpecificationExecutor<AssetTransferRequest> {

    List<AssetTransferRequest> findByAssetIdOrderByCreatedDateDesc(String assetId);

    /** Kiểm tra có request PENDING/APPROVED nào cho asset — dùng để chặn assign trùng. */
    List<AssetTransferRequest> findByAssetIdAndStatusIn(String assetId, List<String> statuses);
}
