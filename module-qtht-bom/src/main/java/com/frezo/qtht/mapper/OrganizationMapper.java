package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.OrganizationAddRequest;
import com.frezo.qtht.dto.request.OrganizationEditRequest;
import com.frezo.qtht.dto.response.OrganizationResponse;
import com.frezo.qtht.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "legalRepresentativeId", source = "legalRepresentativeId")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "updatedDate")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "shortName", source = "shortName")
    @Mapping(target = "nameEn", source = "nameEn")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "logoUrl", source = "logoUrl")
    @Mapping(target = "website", source = "website")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "fax", source = "fax")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "provinceCode", source = "provinceCode")
    @Mapping(target = "wardCode", source = "wardCode")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "orderIndex", source = "orderIndex")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "scale", source = "scale")
    @Mapping(target = "businessSector", source = "businessSector")
    @Mapping(target = "establishedDate", source = "establishedDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "contactPerson", source = "contactPerson")
    @Mapping(target = "contactPosition", source = "contactPosition")
    @Mapping(target = "contactEmail", source = "contactEmail")
    @Mapping(target = "contactPhone", source = "contactPhone")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    OrganizationResponse toResponse(Organization organization);

    @Mapping(target = "parentId", source = "parentId")
    @Mapping(target = "legalRepresentativeId", source = "legalRepresentativeId")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "updatedDate")
    @Mapping(target = "parent", source = "parent")
    com.frezo.qtht.dto.response.OrganizationDetailResponse toDetailResponse(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "legalRepresentative", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "legalRepresentativeId", ignore = true)
    Organization toEntity(OrganizationAddRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "legalRepresentative", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "legalRepresentativeId", ignore = true)
    void updateEntity(OrganizationEditRequest request, @MappingTarget Organization organization);
}
