package com.frezo.common.workflow.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Bản thiết kế quy trình duyệt (template) — dùng chung cho mọi module.
 * <p>
 * Ví dụ:
 * <ul>
 *   <li>{@code code="ASSET_TRANSFER_DEFAULT"} — 2 bước: HR verify → Admin approve</li>
 *   <li>{@code code="LEAVE_DEFAULT"} — 2 bước: Manager → HR</li>
 *   <li>{@code code="CONTRACT_APPROVE"} — 3 bước: Legal → Finance → CEO</li>
 * </ul>
 * <p>
 * Mỗi entity muốn có approval flow chỉ cần chọn 1 {@code definition} khi tạo
 * ticket. Admin có thể sửa steps/approvers bất cứ lúc nào — bản mới sẽ áp dụng
 * cho instance sinh sau; instance đang chạy giữ steps ban đầu (snapshot theo thiết kế
 * BPM chuẩn).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_definition", indexes = {
        @Index(name = "idx_wf_def_code", columnList = "code", unique = true),
        @Index(name = "idx_wf_def_module", columnList = "module_code"),
})
public class WorkflowDefinition extends BaseEntity {

    /** Mã unique — dùng khi start instance. VD: ASSET_TRANSFER_DEFAULT. */
    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * Module mà quy trình này áp dụng. Cho phép filter trong Editor UI.
     * VD: ASSET, LEAVE, CONTRACT, PURCHASE_ORDER, ...
     */
    @Column(name = "module_code", length = 50, nullable = false)
    private String moduleCode;

    @Column(name = "description", length = 1000)
    private String description;

    /** True nếu có thể sử dụng để start instance mới. False = đã deprecate. */
    @Column(name = "active", nullable = false)
    private Boolean active;

    /**
     * Chế độ editor: {@code SIMPLE} (form step cũ) hoặc {@code VISUAL} (React Flow).
     * Default SIMPLE để không phá API /wf cũ.
     */
    @Column(name = "editor_mode", length = 20)
    @Builder.Default
    private String editorMode = "SIMPLE";

    /**
     * Graph visual (lanes/nodes/edges) — JSON string.
     * Chỉ dùng khi {@code editorMode=VISUAL}.
     */
    @Column(name = "graph_json", columnDefinition = "TEXT")
    private String graphJson;

    /** Hướng dẫn quy trình (markdown) — seed từ BE, FE chỉ fetch. */
    @Column(name = "guide_markdown", columnDefinition = "TEXT")
    private String guideMarkdown;

    /**
     * Key mẫu hệ thống (VD {@code RECRUITMENT_DEFAULT}).
     * Gallery list theo field này khi {@code isTemplate=true}.
     */
    @Column(name = "template_key", length = 100)
    private String templateKey;

    /** Code template gốc khi clone từ gallery. */
    @Column(name = "source_template_code", length = 100)
    private String sourceTemplateCode;

    /** Version graph / definition (tăng khi save visual). */
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    /** True = mẫu hệ thống hiện trong gallery (không xoá / không start instance). */
    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;
}
