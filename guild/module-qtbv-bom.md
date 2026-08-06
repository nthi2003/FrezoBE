# Frezo Backend — module-qtbv-bom (CMS bài viết / Banner / Landing)

> Module **QTBV**: quản trị nội dung — bài viết (workflow duyệt), banner, cấu hình landing, API public website.
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) · [module-qtht-bom.md](./module-qtht-bom.md) (Person / Organization lookup) · [module-dmdc-bom.md](./module-dmdc-bom.md) (danh mục — khác module).

Package gốc: `com.frezo.qtbv`. Module Maven: `module-qtbv-bom`.

**Phụ thuộc:** `module-common`, `module-auth-bom`, `module-qtht-bom` (Person, Organization), `module-product-bom` (product public).

**Không có Events** trong module này (không entity/controller Event). Event livestream thuộc module khác (vd `module-fbautomation-bom`).

---

## 1. Phạm vi

| Trong module | Ngoài / ghi chú |
|--------------|-----------------|
| Articles + revision + reaction (service) | Reaction/Vision **chưa** có REST controller |
| Banners | Admin CRUD; **chưa** expose `/public/banners` |
| LandingConfig | Admin + public |
| PublicController | Landing + product + article PUBLIC |
| CommonController | Dropdown manager / org cho form bài |

Docs Hub nội bộ (`/qtht/guides`) thuộc [module-qtht-bom.md](./module-qtht-bom.md) — không nằm QTBV.

---

## 2. Class map

### 2.1 Controllers

| Class | Base path | Auth | Vai trò |
|-------|-----------|------|---------|
| `ArticleController` | `/qtbv/articles` | JWT + `@CheckPermission` | CRUD + submit/review/publish + feed |
| `CommonController` | `/qtbv/articles` | JWT + permission | Lookup managers / organizations |
| `BannerController` | `/qtbv/banners` | JWT + permission | CRUD banner |
| `LandingConfigController` | `/qtbv/landing-config` | JWT + permission | Get / update config |
| `PublicController` | `/public` | **Không auth** | Landing, product, article public |

### 2.2 Services

| Service | Impl | Dùng bởi |
|---------|------|----------|
| `ArticleService` | `ArticleServiceImpl` | ArticleController, PublicService |
| `BannerService` | `BannerServiceImpl` | BannerController |
| `LandingConfigService` | `LandingConfigServiceImpl` | LandingConfigController, PublicService |
| `PublicService` | `PublicServiceImpl` | PublicController |
| `ManagerService` | `ManagerServiceImpl` | CommonController ← Person active |
| `OrganizationCommonService` | `OrganizationCommonServiceImpl` | CommonController ← Organization |
| `ArticleVisionService` | `ArticleVisionServiceImpl` | **Không controller** — manager edit → revision |
| `ArticleReactionService` | `ArticleReactionServiceImpl` | **Không controller** — toggle reaction |

---

## 3. Bài viết — `/qtbv/articles`

### 3.1 API admin / intranet

| Method | Path | Mục đích |
|--------|------|----------|
| POST | `/filter` | Filter body |
| GET | `/` | List/filter query |
| GET | `/{id}` | Chi tiết admin |
| POST | `/` | Tạo — status `DRAFT` |
| PUT | `/{id}` | Sửa — chỉ author, status DRAFT/REJECTED |
| DELETE | `/{id}` | Soft-delete — author |
| PUT | `/{id}/submit` | Gửi duyệt (cần `managerId`) |
| PUT | `/{id}/review` | Duyệt / từ chối (manager chỉ định) |
| PUT | `/{id}/publish` | Xuất bản (manager; chỉ APPROVED) |
| GET | `/my-drafts` | Draft của tôi |
| GET | `/pending-approval` | Hàng đợi WAITING_APPROVAL của manager |
| GET | `/published` | List đã publish (optional `organizationId`) |
| GET | `/home-feed` | Feed intranet (mọi user đăng nhập) |
| GET | `/home-feed/{id}` | Đọc bài published |

### 3.2 Lookups — `CommonController` (cùng prefix `/qtbv/articles`)

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/managers` | Person active → dropdown manager |
| GET | `/organizations` | Organization → dropdown org |

### 3.3 Workflow trạng thái

```
DRAFT ──submit──► WAITING_APPROVAL ──review OK──► APPROVED ──publish──► PUBLISHED
   ▲                      │
   │                   reject
   │                      ▼
   └──────────── REJECTED (sửa + submit lại)
DRAFT / REJECTED ──delete──► DELETED (soft)
```

| Bước | Rule |
|------|------|
| Create | `DRAFT`, `isActive=false`, `publishScope` mặc định `INTERNAL`, sync `isPublic` |
| Update | Chỉ author; chỉ DRAFT/REJECTED |
| Submit | Bắt buộc `managerId`; clear `rejectNote` |
| Review | Chỉ manager được chỉ định; → APPROVED hoặc REJECTED (+ `rejectNote`) |
| Publish | Chỉ manager; APPROVED → PUBLISHED; áp dụng revision mới nhất rồi xóa revisions; `publishedAt`, `isActive=true` |
| Manager edit nội dung đang chờ duyệt | `ArticleVisionService.managerEdit` lưu `ArticleRevision` — **chưa wire REST** |

### 3.4 `publishScope` & visibility

| Scope | `isPublic` | `/qtbv/articles/home-feed` | `/public/articles` |
|-------|------------|----------------------------|--------------------|
| `INTERNAL` | false | Có (đã PUBLISHED) | Không |
| `PUBLIC` | true | Có | Có (PUBLISHED + public) |

### 3.5 Entity `Article` → `articles`

| Field | Ý nghĩa |
|-------|---------|
| `code` | Unique; auto `QTBV-YYYYMMDD-###` nếu bỏ trống |
| `title`, `content` | Nội dung |
| `authorId` | Person tạo |
| `managerId` | Người duyệt / publish |
| `organizationId` | Phạm vi org |
| **`status`** | `DRAFT` \| `WAITING_APPROVAL` \| `APPROVED` \| `PUBLISHED` \| `REJECTED` \| `DELETED` |
| **`publishScope`** | `INTERNAL` \| `PUBLIC` |
| `isActive`, `isPublic` | Flag hiển thị / public |
| `rejectNote` | Lý do từ chối gần nhất |
| `publishedAt`, `deletedAt` | Timestamp |

**`ArticleRevision` → `article_revisions`:** `articleId`, `editorId`, `oldContent`, `newContent`.

**`ArticleReaction` → `article_reactions`:** `articleId`, `userId`, `type` = `HEART` \| `LIKE` \| `DISLIKE`. Toggle service có; aggregate heart dùng khi list/detail — **chưa có endpoint REST trong module**.

Error keys (inline, không enum riêng): `article.code.exists`, `article.permission.*`, `article.status.invalid.*`, `article.not.found`, `article.manager.required`, …

---

## 4. Banner — `/qtbv/banners`

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/` | List (sort `position`, `orderIndex`) |
| GET | `/{id}` | Chi tiết |
| POST | `/` | Tạo |
| PUT | `/{id}` | Sửa |
| DELETE | `/{id}` | Soft-delete |

Entity `Banner` → `banners`: `title`, `subtitle`, `imageUrl` (bắt buộc), `linkUrl`, `position` (mặc định `hero`), `status` (`ACTIVE`/`INACTIVE`), `orderIndex`.

⚠️ Public landing **chưa** đọc banner qua `/public/*` — chỉ admin API.

---

## 5. Landing config — `/qtbv/landing-config`

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/` | Config đang active |
| PUT | `/` | Cập nhật |

Entity `LandingConfig` → `landing_config` (pattern 1 row active):

| Nhóm field | Ví dụ |
|------------|-------|
| Brand | `brandName`, `logoUrl`, `primaryColor` |
| Sections | Hero / blog / product / ops titles, shipping policy |
| Contact | Thông tin liên hệ |
| SEO | `seoTitle`, `seoDescription`, `seoKeywords`, `ogImageUrl`, `faviconUrl`, `canonicalUrl` |
| Social | Facebook, Instagram, YouTube, TikTok, Zalo |
| Analytics | `gtmId`, `ga4Id`, `fbPixelId` |
| Flag | `isActive` |

Public đọc cùng dữ liệu qua `GET /public/landing/config`.

---

## 6. Public API — `/public` (không JWT)

Controller: `PublicController` → `PublicServiceImpl`.

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/public/landing/config` | Config landing |
| POST | `/public/product/filter` | Catalog sản phẩm public |
| GET | `/public/product/{id}` | Chi tiết SP |
| GET | `/public/articles` | Bài PUBLIC + PUBLISHED (paging) |
| GET | `/public/articles/{id}` | Chi tiết bài public |

| Surface | Base | Auth | Phạm vi |
|---------|------|------|---------|
| Public | `/public/*` | Không | Landing, product, article PUBLIC |
| CMS admin | `/qtbv/*` | JWT + permission | Full CRUD / workflow |
| Intranet reader | `/qtbv/articles/home-feed*` | Đăng nhập | Mọi PUBLISHED (INTERNAL+PUBLIC) |

---

## 7. Reaction & Vision (chưa expose REST)

| Service | Hành vi | REST |
|---------|---------|------|
| `ArticleReactionService.toggleReaction` | Toggle HEART/LIKE/DISLIKE | ❌ Chưa |
| `ArticleVisionService.managerEdit` | Khi WAITING_APPROVAL, ghi revision | ❌ Chưa |

Khi wire API mới: cập nhật guild + permission seed.

---

## 8. Events

| Hạng mục | Kết luận |
|----------|----------|
| Event entity / controller trong `module-qtbv-bom` | **Không có** |
| Đừng nhầm | Event FB / livestream → module automation khác |

---

## 9. Bản đồ bảng

| Entity | Bảng |
|--------|------|
| `Article` | `articles` |
| `ArticleRevision` | `article_revisions` |
| `ArticleReaction` | `article_reactions` |
| `Banner` | `banners` |
| `LandingConfig` | `landing_config` |

Kế thừa `BaseEntity` (UUID, soft delete, audit) — [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

---

## 10. Cross-links

| Module | Liên hệ |
|--------|---------|
| [module-qtht-bom.md](./module-qtht-bom.md) | Person (author/manager), Organization; Guide Docs Hub riêng |
| [module-dmdc-bom.md](./module-dmdc-bom.md) | Category master data — **không** nằm QTBV (dù package legacy trùng tên `qtbv` ở dmdc) |
| `module-product-bom` | Product filter/detail trên `/public/product` |
| Setting `isColor` (qtht) | Theme ERP — khác LandingConfig public |

```
[CMS]
  POST /qtbv/articles → DRAFT
  PUT  /{id}/submit → WAITING_APPROVAL
  PUT  /{id}/review → APPROVED | REJECTED
  PUT  /{id}/publish → PUBLISHED
[Intranet]
  GET /qtbv/articles/home-feed
[Website]
  GET /public/landing/config
  GET /public/articles
```

**Lưu ý naming:** `module-dmdc-bom` vẫn dùng package `com.frezo.qtbv` cho Category/Asset — **không** nhầm với CMS bài viết của `module-qtbv-bom`.

---

## 11. Checklist

### 11.1 Đọc code lần đầu

- [ ] `ArticleController` + state machine mục 3.3
- [ ] `publishScope` INTERNAL vs PUBLIC + bảng visibility
- [ ] `CommonController` managers/organizations
- [ ] `BannerController` vs thiếu `/public` banner
- [ ] `LandingConfig` admin + public
- [ ] `PublicController` — không CheckPermission
- [ ] `ArticleReactionService` / `ArticleVisionService` — chưa REST
- [ ] Xác nhận không có Event trong module

### 11.2 Rule

| ✅ | ❌ |
|----|----|
| Review/publish chỉ `managerId` được chỉ định | Cho mọi user APPROVE |
| Public chỉ article PUBLIC+PUBLISHED | Lộ bài INTERNAL ra `/public` |
| Soft-delete bài / banner | Hard delete mất audit |
| Phân biệt `module-qtbv-bom` CMS vs `module-dmdc-bom` package `qtbv` | Import nhầm Category vào CMS |

---

*Cập nhật khi wire REST reaction/vision, expose banner public, thêm Event, hoặc đổi state machine bài viết.*
