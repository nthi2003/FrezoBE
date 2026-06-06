package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.ContractAssginWorkAddRequest;
import com.frezo.qlns.dto.request.ContractAssginWorkEditRequest;
import com.frezo.qlns.dto.response.ContractAsginWorkResponse;
import com.frezo.qlns.entity.ContractAssginWork;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContractAssiginWorkMapper {

    ContractAsginWorkResponse toResponse(ContractAssginWork contractAssginWork);

    List<ContractAsginWorkResponse> toListResponse(List<ContractAssginWork> contractAssginWorks);

    ContractAssginWork toEntity (ContractAssginWorkAddRequest request);

    void updateEntity(ContractAssginWorkEditRequest request, @MappingTarget ContractAssginWork contractAssginWork);

}
