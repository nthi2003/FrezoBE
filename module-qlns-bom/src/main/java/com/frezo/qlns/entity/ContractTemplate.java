package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contract_templates")
public class ContractTemplate extends BaseEntity {
    @Column(name = "template_name", length = 500)
    private String name;

    @Column(name = "contract_type", length = 100)
    private String type;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_object_name", columnDefinition = "TEXT")
    private String fileObjectName;
}
