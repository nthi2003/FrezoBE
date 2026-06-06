package com.frezo.task.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {
    private String code;

    private String name;

    private String category;

    private String color;

}
