package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/** Tính năng 7: Landing page control (bài viết, logo, brand) **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "landing_config")
public class LandingConfig extends BaseEntity {

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "about_us", length = 3000)
    private String aboutUs;

    @Column(name = "footer_text", length = 1000)
    private String footerText;

    @Column(name = "hero_title")
    private String heroTitle;

    @Column(name = "hero_subtitle")
    private String heroSubtitle;

    @Column(name = "blog_title")
    private String blogTitle;

    @Column(name = "blog_subtitle")
    private String blogSubtitle;

    @Column(name = "product_title")
    private String productTitle;

    @Column(name = "product_subtitle")
    private String productSubtitle;

    @Column(name = "ops_title")
    private String opsTitle;

    @Column(name = "ops_subtitle")
    private String opsSubtitle;

    @Column(name = "shipping_policy")
    private String shippingPolicy;

    @Column(name = "contact_address")
    private String contactAddress;

    @Column(name = "working_hours")
    private String workingHours;

    @Column(name = "newsletter_title")
    private String newsletterTitle;

    @Column(name = "newsletter_subtitle")
    private String newsletterSubtitle;

    @Column(name = "is_active")
    private Boolean isActive;
}
