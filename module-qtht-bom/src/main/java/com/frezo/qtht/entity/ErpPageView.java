package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * First-party ERP pageview — route/module usage (không lưu body/PII).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "erp_page_view")
public class ErpPageView extends BaseEntity {

    @Column(name = "username", length = 80)
    private String username;

    @Column(name = "route", length = 300, nullable = false)
    private String route;

    @Column(name = "module_code", length = 60)
    private String moduleCode;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
