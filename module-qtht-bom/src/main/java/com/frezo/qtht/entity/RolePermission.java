package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RolePermission — bảng trung gian liên kết {@link Role} với {@link Permission}.
 * <p>
 * Được sử dụng bởi {@code PermissionRepository.checkPermission()} để xác định user
 * (thông qua {@link UserRole}) có được thực hiện một API cụ thể hay không.
 * <p>
 * <b>Trước Batch H</b>: bảng này chỉ tồn tại dưới dạng native SQL trong query, không có
 * JPA entity → không tạo được service/repository/seed. Batch H tạo entity + repository
 * để có thể quản lý qua UI và seed data.
 */
@Entity
@Table(
        name = "role_permission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_permission",
                columnNames = {"role_id", "permission_id"}
        ),
        indexes = {
                @Index(name = "idx_role_permission_role", columnList = "role_id"),
                @Index(name = "idx_role_permission_permission", columnList = "permission_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission extends BaseEntity {

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "permission_id", nullable = false, length = 36)
    private String permissionId;

    /** App code để hỗ trợ multi-tenant (VD: QTHT, POS...). */
    @Column(name = "app_code", length = 32)
    private String appCode;
}
