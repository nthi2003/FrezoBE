package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fb_groups")
public class FacebookGroup extends BaseEntity {

    @Column(name = "group_id", length = 100, nullable = false, unique = true)
    private String groupId;

    @Column(name = "group_name", length = 500)
    private String groupName;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "group_url", length = 1000)
    private String groupUrl;
}
