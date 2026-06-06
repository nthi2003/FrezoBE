package com.frezo.customer.mapper;

import com.frezo.customer.dto.NCCCertificateDTO;
import com.frezo.customer.dto.request.NCCRequest;
import com.frezo.customer.dto.response.NCCResponse;
import com.frezo.customer.entity.NCC;
import com.frezo.customer.entity.NCCCertificate;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NCCMapper {
    @Mapping(target = "certificates", ignore = true)
    NCCResponse toResponse(NCC ncc);

    NCC toEntity(NCCRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget NCC entity, NCCRequest request);

    NCCCertificateDTO toCertificateDTO(NCCCertificate certificate);

    NCCCertificate toCertificateEntity(NCCCertificateDTO dto);
}
