package com.frezo.warehouse.mapper;

import com.frezo.warehouse.dto.request.TransferCreateRequest;
import com.frezo.warehouse.dto.response.TransferResponse;
import com.frezo.warehouse.entity.StockTransfer;
import com.frezo.warehouse.entity.StockTransferItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface StockTransferMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transferCode", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "totalValue", ignore = true)
    @Mapping(target = "transferredBy", ignore = true)
    @Mapping(target = "transferredAt", ignore = true)
    StockTransfer toEntity(TransferCreateRequest request);

    TransferResponse toResponse(StockTransfer transfer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transferId", ignore = true)
    StockTransferItem toItemEntity(TransferCreateRequest.TransferItemRequest request);

    @Mapping(target = "transferId", ignore = true)
    TransferResponse.TransferItemResponse toItemResponse(StockTransferItem item);
}
