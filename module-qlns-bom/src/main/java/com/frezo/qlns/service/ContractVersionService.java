package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.ContractDiffResponse;
import com.frezo.qlns.dto.response.ContractSnapshotResponse;
import com.frezo.qlns.dto.response.ContractVersionListResponse;
import com.frezo.qlns.entity.Contract;

import java.util.List;

public interface ContractVersionService {

    /**
     * Tạo version mới cho contract khi có thay đổi
     */
    ContractSnapshotResponse createVersion(Contract contract, String updateType, String description);

    /**
     * Lấy danh sách tất cả versions của một contract (sắp xếp giảm dần theo version)
     */
    List<ContractVersionListResponse> getVersionsByContractId(String contractId);

    /**
     * So sánh 2 versions để highlight thay đổi
     * @param contractId ID của contract
     * @param fromVersion Version cũ hơn
     * @param toVersion   Version mới hơn
     */
    ContractDiffResponse diffVersions(String contractId, Integer fromVersion, Integer toVersion);
}
