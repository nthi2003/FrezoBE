package com.frezo.common.repository;

import com.frezo.common.entity.AiOcrDocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiOcrDocumentRecordRepository extends JpaRepository<AiOcrDocumentRecord, String> {
}