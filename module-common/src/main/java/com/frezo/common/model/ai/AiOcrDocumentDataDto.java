package com.frezo.common.model.ai;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOcrDocumentDataDto {

    private String documentType;
    private String invoiceNo;
    private String documentDate;
    private BigDecimal totalAmount;
    private String vendor;
    private List<AiOcrLineItemDto> items;
    private String rawText;
    private List<String> warnings;
}