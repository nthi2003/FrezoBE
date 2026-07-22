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

    // ============================================================
    // SEO / Meta — hiển thị trên Google/Facebook share, ảnh hưởng rank
    // ------------------------------------------------------------
    // Nếu để trống, landing page fallback về default hardcoded trong index.html.
    // Khuyến nghị: seoTitle 50-60 ký tự, seoDescription 150-160 ký tự.
    // ============================================================

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;

    /** URL ảnh Open Graph (1200×630px lý tưởng) — Facebook/Zalo share preview. */
    @Column(name = "og_image_url", length = 1000)
    private String ogImageUrl;

    /** URL favicon (SVG/PNG 32×32). */
    @Column(name = "favicon_url", length = 1000)
    private String faviconUrl;

    /** Ảnh nền hero (background chính của trang) — thay vì hardcode /assets/hero-bg.png. */
    @Column(name = "hero_image_url", length = 1000)
    private String heroImageUrl;

    /** URL canonical — cần thiết khi có nhiều domain trỏ về (frezo.vn / www.frezo.vn / frezo.com.vn). */
    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    // ============================================================
    // Social links — hiển thị ở footer
    // ============================================================

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "tiktok_url", length = 500)
    private String tiktokUrl;

    @Column(name = "zalo_url", length = 500)
    private String zaloUrl;

    // ============================================================
    // Analytics / tracking
    // ============================================================

    /** Google Tag Manager container ID (GTM-XXXXXX). */
    @Column(name = "gtm_id", length = 50)
    private String gtmId;

    /** Google Analytics 4 measurement ID (G-XXXXXXXXXX). */
    @Column(name = "ga4_id", length = 50)
    private String ga4Id;

    /** Facebook Pixel ID (chỉ số) — dùng cho remarketing. */
    @Column(name = "fb_pixel_id", length = 50)
    private String fbPixelId;

    @Column(name = "is_active")
    private Boolean isActive;
}
