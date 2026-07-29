# Kế toán — đối soát ngân hàng

Import CSV sao kê → khớp dòng → **Khoá**. Demo **FTECH**.

**Ảnh minh hoạ:** `/docs-assets/eu/` (FTECH demo)

![Frezo · FTECH — topbar *Lê Minh Tuấn*](/docs-assets/eu/topbar.png)

![Menu — **Đối soát ngân hàng**](/docs-assets/eu/acc-sidebar.png)

![Import sao kê — chọn TK + upload](/docs-assets/eu/acc-bank-import.png)

![Wizard 3 bước — Upload · Preview · Confirm](/docs-assets/eu/cta-acc-bank-steps.png)

---

## Import CSV

![Bước 1 — TK *1121 · Tiền gửi VCB FTECH HN*](/docs-assets/eu/acc-bank-import.png)

![Dải bước wizard](/docs-assets/eu/cta-acc-bank-steps.png)

![Màn đối chiếu sau import](/docs-assets/eu/acc-bank-recon.png)

- **Kế toán** → **Đối soát ngân hàng** → **Import CSV** → TK *1121 · Tiền gửi VCB FTECH HN* → upload (ngày · diễn giải · tham chiếu · nợ · có · số dư) → Preview → **Import ngay**
- Đọc thẻ: **Tổng import** · **Đã match** · **Chưa match**

---

## Khớp + Khoá

![Đối chiếu — lọc **Chưa match**](/docs-assets/eu/acc-bank-recon.png)

![Gợi ý khớp Fuzzy / Exact](/docs-assets/eu/acc-bank-recon.png)

- Dòng trái → gợi ý phải (%) → **Match** · nhầm → **Unmatch** · tìm *mô tả / số tiền*
- **Chưa match** = 0 → **Khoá** · cần sửa → **Reopen**
- Không parse CSV → dòng đầu = tên cột · không gợi ý → CT chưa ghi sổ — [Chứng từ](/docs/guide-accounting-journal)
