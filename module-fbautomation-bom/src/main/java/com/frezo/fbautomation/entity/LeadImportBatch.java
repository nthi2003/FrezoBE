package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.time.OffsetDateTime;

/**
 * LeadImportBatch — log 1 lần upload file lead (CSV/Excel).
 * <p>
 * Dùng để:
 * - Audit ai đã upload lead nào, khi nào.
 * - Rollback batch nếu upload nhầm (đánh dấu `rolled_back = true`, các lead
 *   con có `importBatchId` sẽ bị xóa mềm).
 * - Hiển thị lịch sử trên UI.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "lead_import_batches")
public class LeadImportBatch extends BaseEntity {

    /** Tên file upload gốc. */
    @Column(name = "filename", length = 255)
    private String filename;

    /** Nguồn của batch (VD "LANDING_EVENT_2026-Q1", "MANUAL_CSV"). */
    @Column(name = "source", length = 50)
    private String source;

    /** Tổng số dòng trong file (đã trừ header). */
    @Column(name = "row_count")
    private Integer rowCount;

    /** Số lead insert thành công. */
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    /** Số lead skip (trùng phone/email đã tồn tại). */
    @Column(name = "skipped_count")
    @Builder.Default
    private Integer skippedCount = 0;

    /** Số lead lỗi parse (thiếu field bắt buộc, sai format). */
    @Column(name = "failed_count")
    @Builder.Default
    private Integer failedCount = 0;

    /** Log lỗi chi tiết (JSON array of { rowIndex, message }) — tối đa 100 dòng đầu. */
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    /** Username người upload. */
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    /** Đã rollback (xoá mềm các lead con). */
    @Column(name = "rolled_back")
    @Builder.Default
    private Boolean rolledBack = false;
}
