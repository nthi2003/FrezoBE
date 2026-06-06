package com.frezo.qlns.service.Impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.dto.response.ContractDiffResponse;
import com.frezo.qlns.dto.response.ContractSnapshotResponse;
import com.frezo.qlns.dto.response.ContractVersionListResponse;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.ContractVersionHistory;
import com.frezo.qlns.mapper.ContractVersionMapper;
import com.frezo.qlns.repository.ContractVersionHistoryRepository;
import com.frezo.qlns.service.ContractVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ContractVersionServiceImpl implements ContractVersionService {

    private final ContractVersionHistoryRepository versionHistoryRepository;
    private final ContractVersionMapper contractVersionMapper;
    private final ObjectMapper objectMapper;

    // Map tên field -> nhãn tiếng Việt để hiển thị trên UI
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "code", "Mã hợp đồng",
            "name", "Tên hợp đồng",
            "personId", "ID Nhân sự",
            "typeContractId", "Loại hợp đồng",
            "effTo", "Ngày hiệu lực",
            "effFrom", "Ngày hết hết hạn",
            "value", "Giá trị hợp đồng",
            "status", "Trạng thái",
            "activated", "Kích hoạt",
            "htmlContract", "Nội dung hợp đồng"
    );

    @Override
    public ContractSnapshotResponse createVersion(Contract contract, String updateType, String description) {
        try {
            Integer maxVersion = versionHistoryRepository.findMaxVersionByContractId(contract.getId());
            int newVersion = (maxVersion == null) ? 1 : maxVersion + 1;

            String snapshotJson = buildSnapshot(contract);
            ContractVersionHistory history = ContractVersionHistory.builder()
                    .contract(contract)
                    .updateType(updateType)
                    .versionNumber(newVersion)
                    .snapshotJson(snapshotJson)
                    .description(description)
                    .build();

            return contractVersionMapper.toResponse(versionHistoryRepository.save(history));

        } catch (QTHTException e) {
            throw new QTHTException("can.not.create.version");
        }
    }

    @Override
    public List<ContractVersionListResponse> getVersionsByContractId(String contractId) {
        List<ContractVersionHistory> versions =
                versionHistoryRepository.findAllByContractIdOrderByVersionDesc(contractId);

        return versions.stream().map(v -> {
            ContractVersionListResponse dto = new ContractVersionListResponse();
            dto.setId(v.getId());
            dto.setVersionNumber(v.getVersionNumber());
            dto.setUpdateType(v.getUpdateType());
            dto.setDescription(v.getDescription());
            dto.setCreatedBy(v.getCreatedBy());
            dto.setCreatedDate(v.getCreatedDate());
            dto.setSnapshotJson(v.getSnapshotJson());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ContractDiffResponse diffVersions(String contractId, Integer fromVersion, Integer toVersion) {
        ContractVersionHistory from = versionHistoryRepository
                .findByContractIdAndVersionNumber(contractId, fromVersion)
                .orElseThrow(() -> new QTHTException("version.not.found"));

        ContractVersionHistory to = versionHistoryRepository
                .findByContractIdAndVersionNumber(contractId, toVersion)
                .orElseThrow(() -> new QTHTException("version.not.found"));

        ContractDiffResponse diff = new ContractDiffResponse();
        diff.setFromVersion(fromVersion);
        diff.setToVersion(toVersion);
        diff.setFromCreatedBy(from.getCreatedBy());
        diff.setToCreatedBy(to.getCreatedBy());
        diff.setFromCreatedDate(from.getCreatedDate());
        diff.setToCreatedDate(to.getCreatedDate());
        diff.setFromDescription(from.getDescription());
        diff.setToDescription(to.getDescription());

        // So sánh 2 snapshot JSON
        List<ContractDiffResponse.FieldChange> changedFields = compareSnapshots(
                from.getSnapshotJson(),
                to.getSnapshotJson()
        );
        diff.setChangedFields(changedFields);

        return diff;
    }

    /**
     * So sánh 2 snapshot JSON và trả về danh sách các field đã thay đổi
     */
    private List<ContractDiffResponse.FieldChange> compareSnapshots(String fromJson, String toJson) {
        List<ContractDiffResponse.FieldChange> changes = new ArrayList<>();
        try {
            Map<String, Object> fromMap = objectMapper.readValue(fromJson, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> toMap = objectMapper.readValue(toJson, new TypeReference<Map<String, Object>>() {});

            // Duyệt qua tất cả keys trong toMap (version mới)
            Set<String> allKeys = new HashSet<>();
            allKeys.addAll(fromMap.keySet());
            allKeys.addAll(toMap.keySet());

            for (String key : allKeys) {
                // Bỏ qua field id (không có ý nghĩa diff)
                if ("id".equals(key)) continue;

                Object oldVal = fromMap.get(key);
                Object newVal = toMap.get(key);

                String oldStr = oldVal == null ? "" : oldVal.toString();
                String newStr = newVal == null ? "" : newVal.toString();

                // Nếu giá trị thay đổi thì thêm vào danh sách
                if (!Objects.equals(oldStr, newStr)) {
                    String label = FIELD_LABELS.getOrDefault(key, key);
                    changes.add(new ContractDiffResponse.FieldChange(key, label, oldStr, newStr));
                }
            }
        } catch (Exception e) {
            log.error("Error comparing snapshots: {}", e.getMessage());
            throw new QTHTException("can.not.compare.versions");
        }
        return changes;
    }

    private String buildSnapshot(Contract contract) {
        try {
            ContractSnapshotResponse snapshot = contractVersionMapper.toSnapshotResponse(contract);
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new QTHTException("can.not.build.snapshot");
        }
    }

}
