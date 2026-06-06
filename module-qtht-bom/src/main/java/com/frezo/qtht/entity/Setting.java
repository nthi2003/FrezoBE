package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "setting")
public class Setting extends BaseEntity {

    private String orgId;

    private Boolean isEmail;

    private Boolean isSwap; // Đổi ca thay ca

    private Boolean isColor; // set màu sắc website

    private Boolean isAttendance; // bật tắt chấm công

    private String details; // JSON config cho các module (chấm công, lương, ...)

    // New Fields matching Frontend SettingPage
    private String morningStart;
    private String morningEnd;
    private String afternoonStart;
    private String afternoonEnd;

    private Integer maxMembers;
    private Integer maxPosts;

    private Boolean requireAvatar;
    private Boolean requireCV;
    private Boolean requireHealthCert;

    private Boolean autoApproveArticle;
    private String articleApprover;

    private Boolean requireManager;
    private Boolean allowLate;
}
