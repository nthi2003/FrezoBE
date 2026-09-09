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

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "module_code", length = 50, nullable = false)
    private String moduleCode;

    @Column(name = "description", length = 1000)
    private String description;


    @Column(name = "active", nullable = false)
    private Boolean active;


    @Column(name = "editor_mode", length = 20)
    @Builder.Default
    private String editorMode = "SIMPLE";


    @Column(name = "graph_json", columnDefinition = "TEXT")
    private String graphJson;


    @Column(name = "guide_markdown", columnDefinition = "TEXT")
    private String guideMarkdown;


    @Column(name = "template_key", length = 100)
    private String templateKey;


    @Column(name = "source_template_code", length = 100)
    private String sourceTemplateCode;


    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;


    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;
}
