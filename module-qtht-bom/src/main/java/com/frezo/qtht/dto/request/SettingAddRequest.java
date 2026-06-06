package com.frezo.qtht.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingAddRequest {

    private String orgId; // tổ chức

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

    private String details;
}
