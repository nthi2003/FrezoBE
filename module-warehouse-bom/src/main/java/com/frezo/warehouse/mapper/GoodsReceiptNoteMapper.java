package com.frezo.warehouse.mapper;

import com.frezo.warehouse.dto.request.GrnCreateRequest;
import com.frezo.warehouse.dto.response.GrnResponse;
import com.frezo.warehouse.entity.GoodsReceiptNote;
import com.frezo.warehouse.entity.GoodsReceiptNoteItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface GoodsReceiptNoteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "grnCode", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    GoodsReceiptNote toEntity(GrnCreateRequest request);

    GrnResponse toResponse(GoodsReceiptNote grn);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "grnId", ignore = true)
    GoodsReceiptNoteItem toItemEntity(GrnCreateRequest.GrnItemRequest request);

    @Mapping(target = "grnId", ignore = true)
    GrnResponse.GrnItemResponse toItemResponse(GoodsReceiptNoteItem item);
}
