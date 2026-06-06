package com.frezo.qlns.entity;


import com.frezo.common.domain.BaseEntity;
import com.frezo.qlns.common.StatusContarct;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "contract")
public class Contract extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    private String name;

    private String personId;

    @Column(nullable = false, unique = true)
    private String typeContractId;

    private LocalDate effTo ; // Ngày hiệu lực

    private LocalDate effFrom; // Ngày hết hợp đồng

    private Integer value;

    private StatusContarct Status;

    private Boolean activated;

    private String HtmlContract; // CHi tiết hồ sơ


}
