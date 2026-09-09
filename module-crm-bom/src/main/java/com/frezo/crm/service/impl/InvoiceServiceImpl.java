package com.frezo.crm.service.impl;

import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.request.JournalLineRequest;
import com.frezo.accounting.service.JournalService;
import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.common.InvoiceStatus;
import com.frezo.crm.dto.InvoiceRequest;
import com.frezo.crm.entity.Invoice;
import com.frezo.crm.entity.InvoiceItem;
import com.frezo.crm.repository.InvoiceItemRepository;
import com.frezo.crm.repository.InvoiceRepository;
import com.frezo.crm.service.CommissionService;
import com.frezo.crm.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final InvoiceItemRepository itemRepo;
    private final JournalService journalService;
    private final CommissionService commissionService;

    /**
     * Tạo mới hóa đơn bán hàng kèm các mục (items), tự động tính tổng và áp dụng hoa hồng.
     *
     * @param r yêu cầu chứa thông tin hóa đơn và danh sách mục
     * @return hóa đơn vừa được tạo
     */
    @Override
    @Transactional
    public Invoice create(InvoiceRequest r) {
        Invoice inv = Invoice.builder()
                .code(nextCode())
                .customerId(r.getCustomerId())
                .customerName(r.getCustomerName())
                .quoteId(r.getQuoteId())
                .issuedDate(r.getIssuedDate() != null ? r.getIssuedDate() : LocalDate.now())
                .dueDate(r.getDueDate())
                .currency(r.getCurrency() != null ? r.getCurrency() : "VND")
                .status(r.getStatus() != null ? r.getStatus() : InvoiceStatus.DRAFT)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .notes(r.getNotes())
                .build();
        inv.setIsDeleted(false);
        invoiceRepo.save(inv);
        saveItems(inv, r.getItems());
        recalcTotals(inv);
        commissionService.applyCommissionFields(inv, r.getSalespersonUsername(), r.getCommissionRatePercent());
        return invoiceRepo.save(inv);
    }

    /**
     * Cập nhật hóa đơn: thông tin chung và danh sách mục.
     * Không cho phép cập nhật nếu hóa đơn đã được ghi sổ kế toán.
     *
     * @param id mã hóa đơn
     * @param r yêu cầu cập nhật
     * @return hóa đơn đã cập nhật
     */
    @Override
    @Transactional
    public Invoice update(String id, InvoiceRequest r) {
        Invoice inv = get(id);
        if (inv.getGlJournalEntryId() != null) {
            throw new AppException(CrmErrorCode.INVOICE_ALREADY_POSTED, id);
        }
        inv.setCustomerId(r.getCustomerId());
        inv.setCustomerName(r.getCustomerName());
        inv.setQuoteId(r.getQuoteId());
        if (r.getIssuedDate() != null) inv.setIssuedDate(r.getIssuedDate());
        inv.setDueDate(r.getDueDate());
        if (r.getCurrency() != null) inv.setCurrency(r.getCurrency());
        if (r.getStatus() != null) inv.setStatus(r.getStatus());
        inv.setNotes(r.getNotes());
        if (r.getItems() != null) {
            itemRepo.deleteByInvoiceId(inv.getId());
            saveItems(inv, r.getItems());
        }
        recalcTotals(inv);
        commissionService.applyCommissionFields(inv, r.getSalespersonUsername(), r.getCommissionRatePercent());
        return invoiceRepo.save(inv);
    }

    /** Xóa mềm hóa đơn theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        Invoice inv = get(id);
        inv.setIsDeleted(true);
        invoiceRepo.save(inv);
    }

    /** Lấy thông tin hóa đơn theo mã định danh, ném ngoại lệ nếu không tìm thấy. */
    @Override
    @Transactional(readOnly = true)
    public Invoice get(String id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new AppException(CrmErrorCode.INVOICE_NOT_FOUND, id));
    }

    /** Lấy danh sách tất cả hóa đơn đang hoạt động, sắp xếp theo ngày phát hành giảm dần. */
    @Override
    @Transactional(readOnly = true)
    public List<Invoice> list() {
        return invoiceRepo.findByIsDeletedFalseOrderByIssuedDateDesc();
    }

    /** Lấy danh sách mục (items) của hóa đơn, sắp xếp theo số thứ tự dòng. */
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItem> items(String invoiceId) {
        return itemRepo.findByInvoiceIdOrderByLineNoAsc(invoiceId);
    }

    /** Phát hành hóa đơn: chuyển trạng thái từ DRAFT sang ISSUED nếu đang ở trạng thái DRAFT. */
    @Override
    @Transactional
    public Invoice issue(String id) {
        Invoice inv = get(id);
        if (inv.getStatus() == InvoiceStatus.DRAFT) {
            inv.setStatus(InvoiceStatus.ISSUED);
        }
        return invoiceRepo.save(inv);
    }

    /**
     * Ghi nhận thanh toán cho hóa đơn: cập nhật số tiền đã thu, trạng thái hóa đơn,
     * tạo bút toán kế toán thu tiền và tính toán hoa hồng.
     *
     * @param id mã hóa đơn
     * @param amount số tiền thanh toán
     * @param paymentAccountCode mã tài khoản ngân hàng/quỹ nhận tiền
     * @return hóa đơn đã cập nhật
     */
    @Override
    @Transactional
    public Invoice recordPayment(String id, BigDecimal amount, String paymentAccountCode) {
        Invoice inv = get(id);
        BigDecimal newPaid = safe(inv.getPaidAmount()).add(safe(amount));
        inv.setPaidAmount(newPaid);
        if (newPaid.compareTo(inv.getTotal()) >= 0) {
            inv.setStatus(InvoiceStatus.PAID);
        } else if (newPaid.signum() > 0) {
            inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepo.save(inv);

        // Ghi bút toán thu tiền: Nợ 111/112 (paymentAccountCode) | Có 131
        List<JournalLineRequest> lines = new ArrayList<>();
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountCode(paymentAccountCode != null ? paymentAccountCode : "1121");
        dr.setDebit(safe(amount));
        dr.setCredit(BigDecimal.ZERO);
        dr.setDescription("Thu tiền HĐ " + inv.getCode());
        lines.add(dr);

        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountCode("131");
        cr.setDebit(BigDecimal.ZERO);
        cr.setCredit(safe(amount));
        cr.setDescription("Thu tiền KH");
        cr.setPartnerType("CUSTOMER");
        cr.setPartnerId(inv.getCustomerId());
        cr.setPartnerName(inv.getCustomerName());
        lines.add(cr);

        JournalEntryRequest req = new JournalEntryRequest();
        req.setPostingDate(LocalDate.now());
        req.setDocumentDate(LocalDate.now());
        req.setDescription("Thu tiền HĐ " + inv.getCode());
        req.setSourceType(PostingSource.CASH_BANK);
        req.setSourceId(inv.getId());
        req.setIdempotencyKey("invoice-payment:" + inv.getId() + ":" + System.currentTimeMillis());
        req.setLines(lines);
        journalService.createAndPost(req);

        try {
            commissionService.accrueFromInvoice(inv);
            invoiceRepo.save(inv);
        } catch (Exception e) {
            log.warn("Commission accrue failed for invoice {}: {}", inv.getCode(), e.getMessage());
        }

        return inv;
    }

    /**
     * Ghi sổ kế toán cho hóa đơn: tạo bút toán Nợ 131 (Phải thu KH),
     * Có 5113 (Doanh thu) và Có 33311 (Thuế GTGT đầu ra).
     *
     * @param id mã hóa đơn
     * @return hóa đơn đã ghi sổ
     */
    @Override
    @Transactional
    public Invoice postToGL(String id) {
        Invoice inv = get(id);
        if (inv.getGlJournalEntryId() != null) {
            log.info("Invoice {} already posted → return existing", inv.getCode());
            return inv;
        }
        if (inv.getTotal() == null || inv.getTotal().signum() == 0) {
            throw new AppException(CrmErrorCode.INVOICE_NOT_FOUND, "Invoice has no amount");
        }
        List<JournalLineRequest> lines = new ArrayList<>();

        // Nợ 131 (Phải thu KH) — tổng total
        JournalLineRequest dr = new JournalLineRequest();
        dr.setAccountCode("131");
        dr.setDebit(safe(inv.getTotal()));
        dr.setCredit(BigDecimal.ZERO);
        dr.setDescription("Phải thu KH " + inv.getCode());
        dr.setPartnerType("CUSTOMER");
        dr.setPartnerId(inv.getCustomerId());
        dr.setPartnerName(inv.getCustomerName());
        lines.add(dr);

        // Có 5113 (leaf postable) — subtotal chưa thuế.
        // Không dùng 511: trên TT133 đây là TK cha (postable=false) → ACCOUNT_NOT_POSTABLE.
        JournalLineRequest cr = new JournalLineRequest();
        cr.setAccountCode("5113");
        cr.setDebit(BigDecimal.ZERO);
        cr.setCredit(safe(inv.getSubtotal()));
        cr.setDescription("Doanh thu bán hàng HĐ " + inv.getCode());
        lines.add(cr);

        // Có 3331 — thuế GTGT đầu ra
        if (safe(inv.getTaxAmount()).signum() > 0) {
            JournalLineRequest crTax = new JournalLineRequest();
            crTax.setAccountCode("33311");
            crTax.setDebit(BigDecimal.ZERO);
            crTax.setCredit(safe(inv.getTaxAmount()));
            crTax.setDescription("Thuế GTGT đầu ra HĐ " + inv.getCode());
            lines.add(crTax);
        }

        JournalEntryRequest req = new JournalEntryRequest();
        req.setPostingDate(inv.getIssuedDate());
        req.setDocumentDate(inv.getIssuedDate());
        req.setDescription("Bán hàng theo HĐ " + inv.getCode()
                + (inv.getCustomerName() != null ? " – " + inv.getCustomerName() : ""));
        req.setSourceType(PostingSource.SALES_INVOICE);
        req.setSourceId(inv.getId());
        req.setIdempotencyKey("invoice:" + inv.getId());
        req.setLines(lines);
        var response = journalService.createAndPost(req);

        inv.setGlJournalEntryId(response.getId());
        if (inv.getStatus() == InvoiceStatus.DRAFT) inv.setStatus(InvoiceStatus.ISSUED);
        return invoiceRepo.save(inv);
    }

    // ---- Helpers ----

    /** Lưu danh sách mục (items) vào cơ sở dữ liệu cho hóa đơn đã cho. */
    private void saveItems(Invoice inv, List<InvoiceRequest.Item> items) {
        if (items == null) return;
        int lineNo = 1;
        for (InvoiceRequest.Item i : items) {
            BigDecimal qty = safe(i.getQuantity());
            BigDecimal price = safe(i.getUnitPrice());
            BigDecimal gross = qty.multiply(price);
            InvoiceItem it = InvoiceItem.builder()
                    .invoiceId(inv.getId())
                    .lineNo(lineNo++)
                    .productId(i.getProductId())
                    .productName(i.getProductName())
                    .quantity(qty)
                    .unit(i.getUnit() != null ? i.getUnit() : "cái")
                    .unitPrice(price)
                    .taxRate(i.getTaxRate())
                    .lineTotal(gross)
                    .build();
            it.setIsDeleted(false);
            itemRepo.save(it);
        }
    }

    /** Tính lại tổng cộng hóa đơn: tổng tiền chưa thuế, thuế và tổng cộng. */
    private void recalcTotals(Invoice inv) {
        List<InvoiceItem> items = itemRepo.findByInvoiceIdOrderByLineNoAsc(inv.getId());
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (InvoiceItem it : items) {
            sub = sub.add(safe(it.getLineTotal()));
            BigDecimal rate = safe(it.getTaxRate());
            tax = tax.add(safe(it.getLineTotal()).multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        inv.setSubtotal(sub);
        inv.setTaxAmount(tax);
        inv.setDiscountAmount(BigDecimal.ZERO);
        inv.setTotal(sub.add(tax));
    }

    /** Trả về BigDecimal.ZERO nếu giá trị null. */
    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** Tạo mã hóa đơn tự động theo định dạng INV-{năm}-{6 ký tự ngẫu nhiên}. */
    private String nextCode() {
        return "INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
