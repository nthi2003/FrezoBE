package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category extends BaseEntity {


    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "name_en", length = 500)
    private String nameEn;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "short_name", length = 50)
    private String shortName;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "parent_code", length = 50)
    private String parentCode;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "group_code", length = 50)
    private String groupCode;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "order_index")
    private Integer orderIndex;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "description", length = 500)
    private String description;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "active")
    private Boolean active;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_code", referencedColumnName = "code", insertable = false, updatable = false)
    private CategoryGroup categoryGroup;
}
