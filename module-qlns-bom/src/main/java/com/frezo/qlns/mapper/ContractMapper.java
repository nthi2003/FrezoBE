package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.ContractAddRequest;
import com.frezo.qlns.dto.request.ContractEditRequest;
import com.frezo.qlns.dto.response.ContractComboboxResponse;
import com.frezo.qlns.dto.response.ContractResponse;
import com.frezo.qlns.entity.Contract;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContractMapper {

    ContractResponse toResponse(Contract contract);

    Contract toEntity(ContractAddRequest request);

    void updateEntity(ContractEditRequest request, @MappingTarget Contract contract);

    List<ContractComboboxResponse> toListComboboxResponse(List<Contract> contracts);
}
