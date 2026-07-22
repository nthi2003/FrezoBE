package com.frezo.crm.common;

public enum LeadStatus {
    NEW,          // chưa liên hệ
    CONTACTED,    // đã gọi/email
    QUALIFIED,    // đủ điều kiện chuyển thành Deal
    UNQUALIFIED,  // loại
    CONVERTED     // đã convert sang Deal
}
