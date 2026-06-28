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
    private String name;

    private String type;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_object_name")
    private String fileObjectName;
}
