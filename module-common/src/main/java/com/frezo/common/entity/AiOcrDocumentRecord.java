package com.frezo.common.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_ocr_document_record")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOcrDocumentRecord extends BaseEntity {

    @Column(name = "source_file_name", nullable = false)
    private String sourceFileName;

    @Column(name = "review_status", nullable = false)
    private String reviewStatus;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "document_date")
    private String documentDate;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Lob
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Lob
    @Column(name = "warnings_json", columnDefinition = "TEXT")
    private String warningsJson;

    @Lob
    @Column(name = "extracted_data_json", columnDefinition = "TEXT", nullable = false)
    private String extractedDataJson;

    @Lob
    @Column(name = "reviewed_data_json", columnDefinition = "TEXT")
    private String reviewedDataJson;

    @Lob
    @Column(name = "operator_note", columnDefinition = "TEXT")
    private String operatorNote;
}