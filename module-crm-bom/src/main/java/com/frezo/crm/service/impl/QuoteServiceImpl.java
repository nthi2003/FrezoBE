package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.common.QuoteStatus;
import com.frezo.crm.dto.QuoteRequest;
import com.frezo.crm.entity.Quote;
import com.frezo.crm.entity.QuoteItem;
import com.frezo.crm.repository.QuoteItemRepository;
import com.frezo.crm.repository.QuoteRepository;
import com.frezo.crm.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepo;
    private final QuoteItemRepository itemRepo;

    /**
     * Tạo mới báo giá (quote) kèm các mục, tự động tính tổng tiền chưa thuế, thuế và tổng cộng.
     *
     * @param r yêu cầu chứa thông tin báo giá và danh sách mục
     * @return báo giá vừa được tạo
     */
    @Override
    @Transactional
    public Quote create(QuoteRequest r) {
        Quote q = Quote.builder()
                .code(nextCode())
                .dealId(r.getDealId())
                .customerId(r.getCustomerId())
                .issuedDate(r.getIssuedDate() != null ? r.getIssuedDate() : LocalDate.now())
                .validUntil(r.getValidUntil())
                .currency(r.getCurrency() != null ? r.getCurrency() : "VND")
                .status(r.getStatus() != null ? r.getStatus() : QuoteStatus.DRAFT)
                .notes(r.getNotes())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();
        q.setIsDeleted(false);
        quoteRepo.save(q);
        saveItems(q, r.getItems());
        recalcTotals(q);
        return quoteRepo.save(q);
    }

    /**
     * Cập nhật báo giá: thông tin chung và danh sách mục.
     * Xóa cũ rồi tạo lại các mục nếu danh sách mới được cung cấp.
     *
     * @param id mã báo giá
     * @param r yêu cầu cập nhật
     * @return báo giá đã cập nhật
     */
    @Override
    @Transactional
    public Quote update(String id, QuoteRequest r) {
        Quote q = get(id);
        q.setDealId(r.getDealId());
        q.setCustomerId(r.getCustomerId());
        if (r.getIssuedDate() != null) q.setIssuedDate(r.getIssuedDate());
        q.setValidUntil(r.getValidUntil());
        if (r.getCurrency() != null) q.setCurrency(r.getCurrency());
        if (r.getStatus() != null) q.setStatus(r.getStatus());
        q.setNotes(r.getNotes());
        if (r.getItems() != null) {
            itemRepo.deleteByQuoteId(q.getId());
            saveItems(q, r.getItems());
        }
        recalcTotals(q);
        return quoteRepo.save(q);
    }

    /** Xóa mềm báo giá theo mã định danh. */
    @Override
    @Transactional
    public void delete(String id) {
        Quote q = get(id);
        q.setIsDeleted(true);
        quoteRepo.save(q);
    }

    /** Lấy thông tin báo giá theo mã định danh, ném ngoại lệ nếu không tìm thấy. */
    @Override
    @Transactional(readOnly = true)
    public Quote get(String id) {
        return quoteRepo.findById(id)
                .orElseThrow(() -> new AppException(CrmErrorCode.QUOTE_NOT_FOUND, id));
    }

    /** Lấy danh sách tất cả báo giá đang hoạt động, sắp xếp theo ngày phát hành giảm dần. */
    @Override
    @Transactional(readOnly = true)
    public List<Quote> list() {
        return quoteRepo.findByIsDeletedFalseOrderByIssuedDateDesc();
    }

    /** Lấy danh sách mục (items) của báo giá, sắp xếp theo số thứ tự dòng. */
    @Override
    @Transactional(readOnly = true)
    public List<QuoteItem> items(String quoteId) {
        return itemRepo.findByQuoteIdOrderByLineNoAsc(quoteId);
    }

    /** Đánh dấu báo giá đã gửi cho khách hàng, chuyển trạng thái sang SENT. */
    @Override
    @Transactional
    public Quote send(String id) { return changeStatus(id, QuoteStatus.SENT); }

    /** Đánh dấu báo giá đã được khách hàng chấp nhận, chuyển trạng thái sang ACCEPTED. */
    @Override
    @Transactional
    public Quote accept(String id) { return changeStatus(id, QuoteStatus.ACCEPTED); }

    /** Đánh dấu báo giá bị khách hàng từ chối, chuyển trạng thái sang REJECTED. */
    @Override
    @Transactional
    public Quote reject(String id) { return changeStatus(id, QuoteStatus.REJECTED); }

    /** Thay đổi trạng thái báo giá theo mã định danh. */
    private Quote changeStatus(String id, QuoteStatus st) {
        Quote q = get(id);
        q.setStatus(st);
        return quoteRepo.save(q);
    }

    /** Lưu danh sách mục vào cơ sở dữ liệu cho báo giá đã cho, áp dụng giảm giá theo dòng. */
    private void saveItems(Quote q, List<QuoteRequest.Item> items) {
        if (items == null) return;
        int lineNo = 1;
        for (QuoteRequest.Item i : items) {
            BigDecimal qty = safe(i.getQuantity());
            BigDecimal price = safe(i.getUnitPrice());
            BigDecimal disc = safe(i.getDiscountPct());
            BigDecimal gross = qty.multiply(price);
            BigDecimal afterDisc = gross.subtract(gross.multiply(disc).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            QuoteItem it = QuoteItem.builder()
                    .quoteId(q.getId())
                    .lineNo(lineNo++)
                    .productId(i.getProductId())
                    .productName(i.getProductName())
                    .quantity(qty)
                    .unit(i.getUnit() != null ? i.getUnit() : "cái")
                    .unitPrice(price)
                    .taxRate(i.getTaxRate())
                    .discountPct(disc)
                    .lineTotal(afterDisc)
                    .description(i.getDescription())
                    .build();
            it.setIsDeleted(false);
            itemRepo.save(it);
        }
    }

    /** Tính lại tổng cộng báo giá: tổng tiền chưa thuế, thuế và tổng cộng. */
    private void recalcTotals(Quote q) {
        List<QuoteItem> items = itemRepo.findByQuoteIdOrderByLineNoAsc(q.getId());
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal discAmount = BigDecimal.ZERO;
        for (QuoteItem it : items) {
            sub = sub.add(safe(it.getLineTotal()));
            BigDecimal rate = safe(it.getTaxRate());
            tax = tax.add(safe(it.getLineTotal()).multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }
        q.setSubtotal(sub);
        q.setTaxAmount(tax);
        q.setDiscountAmount(discAmount);
        q.setTotal(sub.add(tax));
    }

    /** Trả về BigDecimal.ZERO nếu giá trị null. */
    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** Tạo mã báo giá tự động theo định dạng Q-{năm}-{6 ký tự ngẫu nhiên}. */
    private String nextCode() {
        return "Q-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
