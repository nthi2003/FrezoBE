package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.ContractAddRequest;
import com.frezo.qlns.dto.request.ContractEditRequest;
import com.frezo.qlns.dto.request.ContractHisAddRequest;
import com.frezo.qlns.dto.request.ContractHisEditRequest;
import com.frezo.qlns.dto.response.ContractHisResponse;
import com.frezo.qlns.dto.response.ContractResponse;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.ContractHistory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContractHisMapper {
    ContractHisResponse toResponse(ContractHistory contractHistory);

    List<ContractHisResponse> toListResponse(List<ContractHistory> contractHistory);

    Contract toEntity (ContractHisAddRequest request);

    void updateEntity(ContractHisEditRequest request, @MappingTarget Contract contract);
}
