package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qlns.common.StatusContarct;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "contract_history")
public class ContractHistory extends BaseEntity {

    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_contarct", length = 32)
    private StatusContarct statusContarct;

    private String versionId;

}
