package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contract_version_history", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ContractVersionHistory  extends BaseEntity
{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, referencedColumnName = "id")
    private Contract contract;

    @Column(name = "update_type", length = 20, nullable = false)
    private String updateType;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "snapshot_json", length = 500)
    private String snapshotJson;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
