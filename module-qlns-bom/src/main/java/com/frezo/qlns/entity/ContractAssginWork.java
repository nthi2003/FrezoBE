package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "contract_assgin_work")
public class ContractAssginWork extends BaseEntity {

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "assign_rv")
    private String assignRV; // người xét duyệt

    @Column(name = "assign_op")
    private String assignOP; // người thực hiện

    @Column(name = "assign_person")
    private String assignPerson;  // Nhân viện trong hồ sơ ( thử việc / chính thức)

}
