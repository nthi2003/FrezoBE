package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Job Requisition — nhu cầu tuyển dụng do trưởng phòng khởi tạo.
 * <p>Status flow: {@code OPEN → (ON_HOLD ↔ OPEN) → FILLED | CLOSED}.
 * <p>Khi {@code FILLED} hoặc {@code CLOSED}: không cho apply mới.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recr_requisition", indexes = {
        @Index(name = "idx_req_status", columnList = "status"),
        @Index(name = "idx_req_dept", columnList = "department_id")
})
public class Requisition extends BaseEntity {

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Fresher / Junior / Middle / Senior / Lead. */
    @Column(name = "level", length = 30)
    private String level;

    @Column(name = "min_salary", precision = 18, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 18, scale = 2)
    private BigDecimal maxSalary;

    /** OPEN / ON_HOLD / FILLED / CLOSED. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "hiring_manager_username", length = 100)
    private String hiringManagerUsername;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "requirements", length = 2000)
    private String requirements;
}
