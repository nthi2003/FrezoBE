package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "person", indexes = {
        @Index(name = "idx_person_email", columnList = "email")
})
public class Person extends BaseEntity {

    @Column(length = 20)
    private String code;

    @Column(length = 500, nullable = false)
    private String name;

    @Column(name = "short_name", length = 30)
    private String shortName;

    @Column(nullable = false)
    private Boolean activated;

    @Column(length = 50)
    private String gender;

    private LocalDate dob;

    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "job_title", length = 50)
    private String jobTitle;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(length = 500)
    private String address;

    @Column(length = 2000)
    private String description;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department_id", insertable = false, updatable = false)
    private String departmentId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Organization organization;

    @Column(name = "org_id", insertable = false, updatable = false)
    private String orgId;
}
