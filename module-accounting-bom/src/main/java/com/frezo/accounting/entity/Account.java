package com.frezo.accounting.entity;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.common.AccountingStandard;
import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Tài khoản trong Hệ thống tài khoản kế toán (Chart of Accounts - COA).
 * <p>Được seed theo Thông tư 133/2016 hoặc Thông tư 99/2025 khi khởi tạo dữ liệu.
 * User có thể mở thêm tài khoản cấp con (không thay đổi cấp 1 nếu đã posting).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "acc_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_account_code", columnNames = {"code"}),
        indexes = {
                @Index(name = "idx_acc_account_parent", columnList = "parent_id"),
                @Index(name = "idx_acc_account_type", columnList = "type")
        })
public class Account extends BaseEntity {

    /** Số hiệu tài khoản (VD: "111", "1111", "331", "3383"). */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    /** Tên tài khoản (VD: "Tiền mặt", "Phải trả CBCNV"). */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Bản chất tài khoản (ASSET / LIABILITY / EQUITY / REVENUE / EXPENSE / CLEARING). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    /** Chuẩn kế toán mà tài khoản này thuộc về. */
    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false, length = 10)
    private AccountingStandard standard;

    /** Cấp tài khoản (1, 2, 3). */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** ID tài khoản cha (null nếu là cấp 1). */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    /** Cho phép ghi trực tiếp vào TK này (false với TK cha có con). */
    @Column(name = "postable", nullable = false)
    private Boolean postable;

    /** Yêu cầu chi tiết đối tượng (customer/supplier/employee) — VD 131, 331, 334. */
    @Column(name = "requires_partner", nullable = false)
    private Boolean requiresPartner;

    /** Số dư ban đầu (đầu năm tài chính). */
    @Column(name = "opening_balance", precision = 20, scale = 2)
    private java.math.BigDecimal openingBalance;

    /** Tài khoản còn hoạt động không. */
    @Column(name = "active", nullable = false)
    private Boolean active;

    /** Mô tả — hướng dẫn hạch toán. */
    @Column(name = "description", length = 1000)
    private String description;
}
