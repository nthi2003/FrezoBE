package com.frezo.qtbv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_group")
public class CategoryGroup {
    @Id
    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "cat_group", nullable = false)
    private Integer catGroup;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @OneToMany(mappedBy = "categoryGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Category> categories;
}
