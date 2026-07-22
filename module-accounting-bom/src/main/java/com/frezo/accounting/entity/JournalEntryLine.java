package com.frezo.accounting.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Dòng chi tiết của chứng từ ghi sổ. Mỗi line có 1 tài khoản, kèm số tiền Nợ hoặc Có
 * (một trong hai = 0). Cho phép ghi kèm dimension: department, project, partner (đối tượng).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "acc_journal_entry_line",
        indexes = {
                @Index(name = "idx_acc_jel_entry", columnList = "journal_entry_id"),
                @Index(name = "idx_acc_jel_account", columnList = "account_id"),
                @Index(name = "idx_acc_jel_partner", columnList = "partner_id")
        })
public class JournalEntryLine extends BaseEntity {

    @Column(name = "journal_entry_id", nullable = false, length = 36)
    private String journalEntryId;

    /** Số thứ tự line (1, 2, 3...) — giữ nguyên khi in chứng từ. */
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    /** Snapshot số hiệu TK ở thời điểm posting (VD "334", "6421") — dễ query GL không cần join. */
    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    /** Số tiền Nợ (>= 0). Nếu credit > 0 thì debit = 0. */
    @Column(name = "debit", precision = 20, scale = 2, nullable = false)
    private BigDecimal debit;

    @Column(name = "credit", precision = 20, scale = 2, nullable = false)
    private BigDecimal credit;

    /** Diễn giải line. */
    @Column(name = "description", length = 500)
    private String description;

    // -------- Dimensions (tuỳ chọn) --------

    /** ID phòng ban (dept id) — dùng cho phân tích chi phí theo phòng ban. */
    @Column(name = "department_id", length = 36)
    private String departmentId;

    /** Loại đối tượng: CUSTOMER / SUPPLIER / EMPLOYEE. */
    @Column(name = "partner_type", length = 20)
    private String partnerType;

    /** ID đối tượng (customer_id / supplier_id / person_id). Bắt buộc khi TK requiresPartner. */
    @Column(name = "partner_id", length = 36)
    private String partnerId;

    /** Snapshot tên đối tượng khi ghi (tránh phải join sau này). */
    @Column(name = "partner_name", length = 255)
    private String partnerName;

    /** ID dự án / cost center (nếu có). */
    @Column(name = "project_id", length = 36)
    private String projectId;
}
