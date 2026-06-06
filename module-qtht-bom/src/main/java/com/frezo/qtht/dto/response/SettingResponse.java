package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettingResponse {
    private String id;

    private String orgId;

    private Boolean isEmail;

    private Boolean isSwap; // Đổi ca thay ca

    private Boolean isColor; // set màu sắc website

    private Boolean isAttendance; // bật tắt chấm công

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

    private String details; // JSON config cho các module
}
