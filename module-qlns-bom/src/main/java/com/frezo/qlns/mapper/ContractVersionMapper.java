    package com.frezo.qlns.mapper;

    import com.frezo.qlns.dto.request.ContractAssginWorkAddRequest;
    import com.frezo.qlns.dto.request.ContractAssginWorkEditRequest;
    import com.frezo.qlns.dto.request.ContractSnapshotRequest;
    import com.frezo.qlns.dto.response.ContractAsginWorkResponse;
    import com.frezo.qlns.dto.response.ContractSnapshotResponse;
    import com.frezo.qlns.dto.response.ContractVersionListResponse;
    import com.frezo.qlns.entity.Contract;
    import com.frezo.qlns.entity.ContractAssginWork;
    import com.frezo.qlns.entity.ContractVersionHistory;
    import org.mapstruct.Mapper;
    import org.mapstruct.MappingTarget;
    import org.mapstruct.ReportingPolicy;

    import java.util.List;

    @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public interface ContractVersionMapper {
        ContractSnapshotResponse toResponse(ContractVersionHistory contractVersionHistory);

        List<ContractSnapshotResponse> toListResponse(List<ContractVersionHistory> contractVersionHistory);

        ContractVersionHistory toEntity (ContractSnapshotRequest request);

        ContractSnapshotResponse toSnapshotResponse(Contract contract);

        ContractVersionListResponse toVersionListResponse(ContractVersionHistory history);

        List<ContractVersionListResponse> toVersionListResponses(List<ContractVersionHistory> histories);
    }

