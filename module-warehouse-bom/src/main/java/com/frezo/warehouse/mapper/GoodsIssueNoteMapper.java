package com.frezo.warehouse.mapper;

import com.frezo.warehouse.dto.request.GinCreateRequest;
import com.frezo.warehouse.dto.response.GinResponse;
import com.frezo.warehouse.entity.GoodsIssueNote;
import com.frezo.warehouse.entity.GoodsIssueNoteItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface GoodsIssueNoteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ginCode", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "issuedBy", ignore = true)
    @Mapping(target = "issuedAt", ignore = true)
    GoodsIssueNote toEntity(GinCreateRequest request);

    GinResponse toResponse(GoodsIssueNote gin);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ginId", ignore = true)
    GoodsIssueNoteItem toItemEntity(GinCreateRequest.GinItemRequest request);

    @Mapping(target = "ginId", ignore = true)
    GinResponse.GinItemResponse toItemResponse(GoodsIssueNoteItem item);
}
