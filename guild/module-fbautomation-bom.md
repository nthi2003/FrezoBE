# Frezo Backend — module-fbautomation-bom (FB / Marketing / Lead / Affiliate)

> Module **Facebook automation + marketing ops**: tài khoản FB, group, lead inbox (FB/landing/Zalo), automation crawl, social post, ads, comment moderation, livestream, affiliate short-link.
> Đọc cùng [README.md](./README.md) · [module-common.md](./module-common.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).
> Import lead → customer thuộc `module-customer-bom` (chưa có guild riêng).

Package gốc: `com.frezo.fbautomation`. Context path HTTP thường là `/api`.

---

## 1. Phạm vi module (theo prefix API)

| Nhóm | Base path | Nội dung |
|------|-----------|----------|
| FB core | `/fb/accounts`, `/fb/groups`, `/fb/leads`, `/fb/automation` | Account, group, lead nội bộ, scan/join/login |
| Marketing | `/mkt/posts`, `/mkt/ads`, `/mkt/comments`, `/mkt/reviews`, `/mkt/live`, `/mkt/insights`, `/mkt/affiliate`, `/mkt/leads/import` | Post, ads, moderation, livestream, affiliate, import CSV |
| Public | `/public/inbox`, `/public/r` | Lead landing/Zalo (no JWT), redirect affiliate |

**Không thuộc module này:** CRM Lead pipeline (`module-crm-bom` `/crm/...`), event nội bộ (`module-event-bom`).

---

## 2. Class map (tóm tắt)

### 2.1 Controllers

| Controller | Base path | Vai trò |
|------------|-----------|---------|
| `FacebookAccountController` | `/fb/accounts` | CRUD account + cập nhật cookie |
| `FacebookGroupController` | `/fb/groups` | List / get / delete group |
| `FacebookLeadController` | `/fb/leads` | List, assign, import → customer |
| `FacebookAutomationController` | `/fb/automation` | Scan groups, join, login, summary |
| `SocialPostController` | `/mkt/posts` | CRUD + publish / cancel / duplicate |
| `AdCampaignController` | `/mkt/ads` | Chiến dịch quảng cáo |
| `CommentModerationController` | `/mkt/comments` | Rules + moderate comments |
| `PageReviewController` | `/mkt/reviews` | Review page + reply |
| `LivestreamEventController` | `/mkt/live` | Livestream + notify |
| `MktInsightsController` | `/mkt/insights` | Dashboard tổng hợp |
| `AffiliateLinkController` | `/mkt/affiliate` | Short-link KOL + convert |
| `LeadImportController` | `/mkt/leads/import` | Upload CSV/Excel + history |
| `PublicLeadController` | `/public/inbox` | Landing lead + Zalo webhook |
| `PublicRedirectController` | `/public/r` | 302 redirect affiliate |

### 2.2 Entities → bảng

| Entity | Bảng | Ghi chú |
|--------|------|---------|
| `FacebookAccount` | `fb_accounts` | username, password, cookie, proxy, status |
| `FacebookGroup` | `fb_groups` | groupId, relevance, category |
| `FacebookLead` | `fb_leads` | Multi-source inbox |
| `LeadImportBatch` | (batch import) | Rollback theo batch |
| `SocialPost` | (posts) | Lịch đăng / publish |
| `AdCampaign` | (ads) | Campaign ads |
| `CommentModerationRule` / `ModeratedComment` | moderation | Rule + comment đã xử lý |
| `PageReview` | reviews | Đánh giá page |
| `LivestreamEvent` | live | Sự kiện livestream MKT |
| `AffiliateLink` | `affiliate_links` | Short code + UTM + counters |
| `AffiliateClick` | clicks | Mỗi lần click redirect |

---

## 3. FB Account / Group / Automation

### 3.1 `FacebookAccount` → `fb_accounts`

| Field | Ý nghĩa |
|-------|---------|
| `username` | Unique |
| `password` | Lưu credential automation (bảo mật vận hành) |
| `cookie` | Session cookie sau login |
| `proxyIp` | Proxy gắn account |
| `status` | Trạng thái account |
| `userAgent` | UA giả lập |
| `postsToday` | Đếm post trong ngày |

API chính:

| Method | Path |
|--------|------|
| CRUD | `/fb/accounts`, `/fb/accounts/{id}` |
| `PUT` | `/fb/accounts/{id}/cookie` |

### 3.2 `FacebookGroup` → `fb_groups`

| Field | Ý nghĩa |
|-------|---------|
| `groupId` | ID FB group (unique) |
| `groupName`, `memberCount`, `relevanceScore` | Metadata crawl |
| `status`, `category`, `description`, `groupUrl` | Phân loại / URL |

API: `GET` list/detail, `DELETE` — `/fb/groups`.

### 3.3 Automation — `/fb/automation`

| Method | Path | Hành vi |
|--------|------|---------|
| `POST` | `/fb/automation/scan-groups` | Quét group theo keyword / account |
| `POST` | `/fb/automation/join-group` | Join group |
| `POST` | `/fb/automation/login/{accountId}` | Login session → cập nhật cookie |
| `GET` | `/fb/automation/summary` | Tóm tắt trạng thái |

Job (nếu có): package `fbautomation.job` — schedule scan / post.

---

## 4. Lead inbox (đa nguồn)

### 4.1 `FacebookLead` → `fb_leads`

| Field | Ý nghĩa |
|-------|---------|
| `name`, `phone`, `email`, `address` | PII liên hệ |
| `sourceGroupId` / `sourceGroupName` | Nguồn FB group |
| `profileUrl` | Profile FB |
| `status` | NEW / ASSIGNED / IMPORTED / … |
| `importedCustomerId` | Sau khi import sang customer |
| `source` | `FACEBOOK` \| `LANDING` \| `ZALO` \| `MANUAL` (default FACEBOOK) |
| `subject`, `message` | Nội dung inquiry |
| `sourceIp`, `referer` | Audit anti-spam (public) |
| `assignedTo` | Username CSKH |
| `importBatchId` | Batch CSV |

### 4.2 API nội bộ — `/fb/leads`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/fb/leads` | List / filter |
| `GET` | `/fb/leads/{id}` | Chi tiết |
| `DELETE` | `/fb/leads/{id}` | Soft-delete |
| `POST` | `/fb/leads/{id}/assign` | Gán CSKH |
| `POST` | `/fb/leads/{id}/import` | Import 1 lead → customer |
| `POST` | `/fb/leads/import-batch` | Import nhiều |

### 4.3 Import file — `/mkt/leads/import`

| Method | Path | Hành vi |
|--------|------|---------|
| `POST` | `/mkt/leads/import` | Multipart upload |
| `POST` | `/mkt/leads/import/preview` | Preview trước khi commit |
| `GET` | `/mkt/leads/import/history` | Lịch sử batch |
| `DELETE` | `/mkt/leads/import/{batchId}` | Rollback / xóa batch |

---

## 5. Public lead & Zalo — `/public/inbox`

**Không JWT** — whitelist SecurityConfig (`/public/**`).

| Method | Path | Hành vi |
|--------|------|---------|
| `POST` | `/public/inbox/leads` | Landing contact form |
| `GET`/`POST` | `/public/inbox/zalo/webhook` | Zalo OA verify + callback |

### 5.1 Anti-abuse (landing)

| Check | Hành vi khi fail |
|-------|------------------|
| Honeypot `_hp` | Trả 200 giả success (không lộ) |
| Timestamp `_ts` quá nhanh (&lt; 1.5s) | Trả 200 giả |
| IP rate limit (`PublicLeadRateLimiter`) | 429 hoặc giả success (theo impl) |
| Thiếu phone/email theo rule | Validate / reject |

Sau khi lưu lead (`source=LANDING`): notify users cấu hình `frezo.inbox.notify-users` qua `NotificationService`.

### 5.2 Zalo

`ZaloWebhookVerifier` — verify signature trước khi persist lead `source=ZALO`.

---

## 6. Affiliate — short link + redirect

### 6.1 Flow

```
Admin tạo AffiliateLink (code, targetUrl, UTM, KOL)
  → KOL share /api/public/r/{code}  (hoặc path FE proxy)
       → PublicRedirectController: ghi AffiliateClick, 302 → targetUrl + UTM
  → Khi conversion: POST /mkt/affiliate/{code}/convert → tăng conversion/revenue
```

### 6.2 `AffiliateLink` → `affiliate_links`

| Field | Ý nghĩa |
|-------|---------|
| `code` | Slug unique (index `idx_affiliate_code`) |
| `targetUrl` | URL đích |
| `campaign`, `kolName`, `kolContact` | Phân loại / đối soát |
| `utm_*` | Append khi redirect |
| `commissionRate` | % hoa hồng |
| `status` | ACTIVE / PAUSED / EXPIRED |
| `expiresAt` | Hết hạn → 410 |
| `clickCount`, `uniqueClickCount`, `conversionCount` | Counter denorm |
| `revenue`, `commissionPaid` | Doanh thu / đã trả |

API admin: CRUD + dashboard + convert — `/mkt/affiliate`.  
Public: `GET /public/r/{code}`.

---

## 7. Marketing khác (tóm tắt)

| Area | Path | Entity chính |
|------|------|--------------|
| Social posts | `/mkt/posts` | `SocialPost` — publish / cancel / duplicate |
| Ads | `/mkt/ads` | `AdCampaign` |
| Comments | `/mkt/comments` | Rules + `ModeratedComment` |
| Reviews | `/mkt/reviews` | `PageReview` + reply |
| Livestream | `/mkt/live` | `LivestreamEvent` + notify |
| Insights | `/mkt/insights/dashboard` | Aggregate |

---

## 8. Rule kiến trúc

| Rule | Chi tiết |
|------|----------|
| ✅ Public vs auth | `/public/**` không JWT; `/fb/**`, `/mkt/**` cần auth + permission |
| ✅ Lead đa nguồn | Cùng bảng `fb_leads`, phân biệt `source` |
| ✅ Notify | Public lead → `NotificationService` (common), không SMTP trực tiếp |
| ❌ Nhầm CRM Lead | CRM `Lead` entity khác module — pipeline sales riêng |
| ❌ Hard-delete batch nhầm | Dùng import history / rollback theo `importBatchId` |

---

## 9. Checklist đọc code

- [ ] Controllers theo prefix `/fb`, `/mkt`, `/public`
- [ ] `FacebookLead.source` — FACEBOOK / LANDING / ZALO / MANUAL
- [ ] `PublicLeadController` — honeypot + rate limit + notify
- [ ] `AffiliateLink` + `PublicRedirectController` 302
- [ ] `FacebookAutomationController` — login / scan / join
- [ ] Import multipart `/mkt/leads/import`

---

*Cập nhật khi thêm source lead, đổi path public, hoặc flow affiliate convert.*
