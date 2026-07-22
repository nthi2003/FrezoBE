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
@Table(name = "fb_leads")
public class FacebookLead extends BaseEntity {

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "source_group_id", length = 100)
    private String sourceGroupId;

    @Column(name = "source_group_name", length = 500)
    private String sourceGroupName;

    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "imported_customer_id", length = 36)
    private String importedCustomerId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ============================================================
    // Multi-channel inbox — inquiry giờ có thể đến từ nhiều nguồn,
    // không chỉ FB group crawler.
    // ============================================================

    /**
     * Nguồn lead: FACEBOOK | LANDING | ZALO | MANUAL.
     * Default FACEBOOK để giữ backward compat với data cũ.
     */
    @Column(name = "source", length = 20)
    @Builder.Default
    private String source = "FACEBOOK";

    /**
     * Chủ đề / dịch vụ khách quan tâm (form landing có field này).
     */
    @Column(name = "subject", length = 255)
    private String subject;

    /**
     * Nội dung tin nhắn khách gửi (dài hơn note).
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * IP client (audit / anti-spam) — chỉ set khi lead đến qua public endpoint.
     */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /**
     * Referrer URL (landing page URL, Zalo OA ID, FB page ID, v.v.).
     */
    @Column(name = "referer", length = 500)
    private String referer;

    /**
     * User assigned để xử lý inquiry (username).
     */
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    /**
     * ID của batch upload (nếu lead đến từ CSV/Excel import).
     * Cho phép rollback nguyên batch nếu upload nhầm.
     */
    @Column(name = "import_batch_id", length = 36)
    private String importBatchId;
}
