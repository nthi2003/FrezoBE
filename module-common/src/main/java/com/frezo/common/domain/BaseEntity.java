package com.frezo.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base entity — kế thừa bởi MỌI entity JPA trong Frezo.
 * <p>
 * Xem chi tiết: {@code FrezoBE/DATABASE_STANDARD.md §3 — Audit fields}.
 * <p>
 * <b>Audit fields (auto-populate qua {@code AuditingEntityListener}):</b>
 * <ul>
 *   <li>{@code createdBy / createdDate} — set 1 lần khi INSERT, không đổi.</li>
 *   <li>{@code updatedBy / updatedDate} — set mỗi lần UPDATE.</li>
 * </ul>
 * <p>
 * <b>Soft-delete fields:</b>
 * <ul>
 *   <li>{@code isDeleted} — boolean legacy, default false. Query hiện tại filter bằng {@code isDeletedFalse}.</li>
 *   <li>{@code deletedAt} (v1.1) — timestamp khi xoá (nullable). Set thủ công trong service khi soft-delete.</li>
 *   <li>{@code deletedBy} (v1.1) — username người xoá (nullable). Set thủ công.</li>
 * </ul>
 * <p>
 * <b>ID:</b> UUID string 36 chars, tự sinh trong {@link #prePersist()} nếu client không set.
 * <p>
 * <b>Concurrency (@Version):</b> KHÔNG add vào BaseEntity mặc định vì nhiều entity mapping từ DTO qua MapStruct
 * dễ mất version → OptimisticLockException giả. Chỉ opt-in cho entity thực sự concurrent-heavy
 * (Order, GoodsReceiptNote, StockBalance...) — xem {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §11.3}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @CreatedBy
    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    /**
     * v1.1 — Thời điểm soft-delete. Nullable — chỉ set khi thực sự xóa.
     * Migration DB add cột: xem {@code db/migration/README.md} §backlog.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * v1.1 — Username người soft-delete. Nullable.
     */
    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * Helper — đánh dấu entity đã bị soft-delete.
     * Service nên gọi method này thay vì set trực tiếp từng field, để đảm bảo cả 3 field
     * ({@code isDeleted}, {@code deletedAt}, {@code deletedBy}) đồng bộ.
     * <p>
     * <b>Ví dụ:</b>
     * <pre>
     * entity.softDelete(SystemUtils.getCurrentUsername());
     * repository.save(entity);
     * </pre>
     */
    public void softDelete(String username) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = username;
    }
}
