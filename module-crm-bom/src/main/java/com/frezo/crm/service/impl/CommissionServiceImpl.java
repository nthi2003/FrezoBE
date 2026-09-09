package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.entity.*;
import com.frezo.crm.repository.*;
import com.frezo.crm.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private static final BigDecimal FALLBACK_RATE = new BigDecimal("5.00");

    private final CommissionRuleRepository ruleRepo;
    private final CommissionEntryRepository entryRepo;
    private final QuoteRepository quoteRepo;
    private final DealRepository dealRepo;
    private final InvoiceItemRepository itemRepo;

    /** Lấy danh sách tất cả quy tắc hoa hồng đang hoạt động. */
    @Override
    @Transactional(readOnly = true)
    public List<CommissionRule> listRules() {
        return ruleRepo.findByIsDeletedFalseOrderBySalespersonUsernameAsc();
    }

    /**
     * Tạo mới hoặc cập nhật quy tắc hoa hồng cho một nhân viên kinh doanh.
     * Nếu nhân viên đã có quy tắc thì cập nhật, nếu chưa thì tạo mới.
     *
     * @param salespersonUsername tên nhân viên kinh doanh
     * @param ratePercent tỷ lệ hoa hồng (0–100%)
     * @param active trạng thái kích hoạt
     * @param note ghi chú
     * @return quy tắc hoa hồng đã lưu
     */
    @Override
    @Transactional
    public CommissionRule upsertRule(String salespersonUsername, BigDecimal ratePercent, Boolean active, String note) {
        if (!StringUtils.hasText(salespersonUsername)) {
            throw new AppException(CrmErrorCode.COMMISSION_INVALID, "Thiếu username sale");
        }
        if (ratePercent == null || ratePercent.signum() < 0 || ratePercent.compareTo(new BigDecimal("100")) > 0) {
            throw new AppException(CrmErrorCode.COMMISSION_INVALID, "Mức hoa hồng phải từ 0–100%");
        }
        String user = salespersonUsername.trim();
        CommissionRule rule = ruleRepo.findBySalespersonUsernameAndIsDeletedFalse(user)
                .orElseGet(() -> CommissionRule.builder()
                        .salespersonUsername(user)
                        .build());
        rule.setRatePercent(ratePercent.setScale(4, RoundingMode.HALF_UP));
        rule.setActive(active == null || active);
        rule.setNote(note);
        rule.setIsDeleted(false);
        return ruleRepo.save(rule);
    }

    /**
     * Xóa mềm quy tắc hoa hồng theo mã định danh.
     * Không cho xóa quy tắc mặc định.
     *
     * @param id mã định danh quy tắc cần xóa
     */
    @Override
    @Transactional
    public void deleteRule(String id) {
        ruleRepo.findById(id).ifPresent(r -> {
            if (CommissionRule.DEFAULT_USERNAME.equals(r.getSalespersonUsername())) {
                throw new AppException(CrmErrorCode.COMMISSION_INVALID, "Không xoá mức mặc định — hãy sửa %");
            }
            r.setIsDeleted(true);
            r.setActive(false);
            ruleRepo.save(r);
        });
    }

    /**
     * Xác định tỷ lệ hoa hồng cho nhân viên kinh doanh.
     * Ưu tiên quy tắc cá nhân, sau đó dùng quy tắc mặc định, cuối cùng dùng giá trị dự phòng 5%.
     *
     * @param salespersonUsername tên nhân viên kinh doanh (có thể null)
     * @return tỷ lệ hoa hồng %
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveRatePercent(String salespersonUsername) {
        if (StringUtils.hasText(salespersonUsername)) {
            Optional<CommissionRule> personal = ruleRepo.findBySalespersonUsernameAndIsDeletedFalse(salespersonUsername.trim());
            if (personal.isPresent() && Boolean.TRUE.equals(personal.get().getActive())) {
                return personal.get().getRatePercent();
            }
        }
        return ruleRepo.findBySalespersonUsernameAndIsDeletedFalse(CommissionRule.DEFAULT_USERNAME)
                .filter(r -> Boolean.TRUE.equals(r.getActive()))
                .map(CommissionRule::getRatePercent)
                .orElse(FALLBACK_RATE);
    }

    /**
     * Áp dụng thông tin hoa hồng lên hóa đơn: xác định nhân viên kinh doanh, tỷ lệ và số tiền hoa hồng.
     *
     * @param invoice hóa đơn cần áp dụng
     * @param salespersonOverride tên nhân viên ghi đè (có thể null)
     * @param rateOverride tỷ lệ ghi đè (có thể null)
     */
    @Override
    @Transactional
    public void applyCommissionFields(Invoice invoice, String salespersonOverride, BigDecimal rateOverride) {
        String salesperson = StringUtils.hasText(salespersonOverride)
                ? salespersonOverride.trim()
                : resolveSalespersonFromQuote(invoice.getQuoteId());

        if (StringUtils.hasText(salesperson)) {
            invoice.setSalespersonUsername(salesperson);
        }

        BigDecimal rate = rateOverride != null
                ? rateOverride
                : resolveRatePercent(invoice.getSalespersonUsername());
        invoice.setCommissionRatePercent(rate.setScale(4, RoundingMode.HALF_UP));

        BigDecimal base = safe(invoice.getTotal());
        BigDecimal amount = base.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        invoice.setCommissionAmount(amount);
    }

    /**
     * Tính toán và ghi nhận bút toán hoa hồng từ hóa đơn đã thu tiền.
     * Chỉ xử lý khi hóa đơn có nhân viên kinh doanh và số tiền thu > 0.
     * Không ghi đè entry đã thanh toán hoặc đã hủy.
     *
     * @param invoice hóa đơn cần tính hoa hồng
     * @return bản ghi CommissionEntry hoặc null nếu bỏ qua
     */
    @Override
    @Transactional
    public CommissionEntry accrueFromInvoice(Invoice invoice) {
        if (invoice == null || !StringUtils.hasText(invoice.getSalespersonUsername())) {
            log.debug("Skip commission accrue — missing salesperson on invoice {}",
                    invoice != null ? invoice.getCode() : null);
            return null;
        }
        if (invoice.getPaidAmount() == null || invoice.getPaidAmount().signum() <= 0) {
            return null;
        }

        BigDecimal rate = invoice.getCommissionRatePercent() != null
                ? invoice.getCommissionRatePercent()
                : resolveRatePercent(invoice.getSalespersonUsername());
        // Cơ sở = số đã thu (partial) — cập nhật lại khi thu thêm
        BigDecimal base = safe(invoice.getPaidAmount());
        BigDecimal commission = base.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal qty = itemRepo.findByInvoiceIdOrderByLineNoAsc(invoice.getId()).stream()
                .map(i -> safe(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String dealId = null;
        if (StringUtils.hasText(invoice.getQuoteId())) {
            dealId = quoteRepo.findById(invoice.getQuoteId()).map(Quote::getDealId).orElse(null);
        }

        CommissionEntry entry = entryRepo.findByInvoiceIdAndIsDeletedFalse(invoice.getId())
                .orElseGet(() -> CommissionEntry.builder()
                        .invoiceId(invoice.getId())
                        .status("PENDING")
                        .build());

        if ("PAID".equals(entry.getStatus()) || "VOID".equals(entry.getStatus())) {
            // Không ghi đè entry đã trả / huỷ
            return entry;
        }

        entry.setInvoiceCode(invoice.getCode());
        entry.setDealId(dealId);
        entry.setSalespersonUsername(invoice.getSalespersonUsername());
        entry.setBaseAmount(base);
        entry.setRatePercent(rate);
        entry.setCommissionAmount(commission);
        entry.setItemQuantity(qty);
        entry.setAccruedAt(LocalDateTime.now());
        entry.setIsDeleted(false);
        if (!StringUtils.hasText(entry.getStatus())) {
            entry.setStatus("PENDING");
        }

        // Sync snapshot lên invoice
        invoice.setCommissionRatePercent(rate);
        invoice.setCommissionAmount(commission);

        return entryRepo.save(entry);
    }

    /** Lấy danh sách bút toán hoa hồng, lọc theo nhân viên kinh doanh nếu được chỉ định. */
    @Override
    @Transactional(readOnly = true)
    public List<CommissionEntry> listEntries(String salespersonUsername) {
        if (StringUtils.hasText(salespersonUsername)) {
            return entryRepo.findBySalespersonUsernameAndIsDeletedFalseOrderByAccruedAtDesc(salespersonUsername.trim());
        }
        return entryRepo.findByIsDeletedFalseOrderByAccruedAtDesc();
    }

    /** Duyệt bút toán hoa hồng theo mã định danh, chuyển trạng thái sang APPROVED. */
    @Override
    @Transactional
    public CommissionEntry approve(String id) {
        CommissionEntry e = getEntry(id);
        e.setStatus("APPROVED");
        return entryRepo.save(e);
    }

    /** Đánh dấu bút toán hoa hồng đã thanh toán, chuyển trạng thái sang PAID. */
    @Override
    @Transactional
    public CommissionEntry markPaid(String id) {
        CommissionEntry e = getEntry(id);
        e.setStatus("PAID");
        return entryRepo.save(e);
    }

    /** Hủy bút toán hoa hồng theo mã định danh, chuyển trạng thái sang VOID. */
    @Override
    @Transactional
    public CommissionEntry voidEntry(String id) {
        CommissionEntry e = getEntry(id);
        e.setStatus("VOID");
        return entryRepo.save(e);
    }

    /**
     * Tổng hợp dữ liệu bảng điều khiển hoa hồng: tổng tiền, số hóa đơn, số lượng,
     * và breakdown theo từng nhân viên kinh doanh.
     *
     * @return map chứa tổng quan và danh sách theo nhân viên
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<Map<String, Object>> bySale = new ArrayList<>();
        BigDecimal totalCommission = BigDecimal.ZERO;
        long totalInvoices = 0;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Object[] row : entryRepo.dashboardBySalesperson()) {
            Map<String, Object> m = new LinkedHashMap<>();
            String uname = row[0] != null ? row[0].toString() : "—";
            long invCount = ((Number) row[1]).longValue();
            BigDecimal comm = toBd(row[2]);
            BigDecimal base = toBd(row[3]);
            BigDecimal qty = toBd(row[4]);
            m.put("salespersonUsername", uname);
            m.put("invoiceCount", invCount);
            m.put("totalCommission", comm);
            m.put("totalBase", base);
            m.put("totalQuantity", qty);
            bySale.add(m);
            totalCommission = totalCommission.add(comm);
            totalInvoices += invCount;
            totalQty = totalQty.add(qty);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalCommission", totalCommission);
        out.put("totalInvoices", totalInvoices);
        out.put("totalQuantity", totalQty);
        out.put("salespersonCount", bySale.size());
        out.put("defaultRatePercent", resolveRatePercent(null));
        out.put("bySalesperson", bySale);
        return out;
    }

    /** Lấy bút toán hoa hồng theo mã, kiểm tra chưa bị xóa mềm và tồn tại. */
    private CommissionEntry getEntry(String id) {
        return entryRepo.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new AppException(CrmErrorCode.COMMISSION_NOT_FOUND, id));
    }

    /** Xác định nhân viên kinh doanh từ báo giá thông qua chain quote → deal → owner. */
    private String resolveSalespersonFromQuote(String quoteId) {
        if (!StringUtils.hasText(quoteId)) return null;
        return quoteRepo.findById(quoteId)
                .map(Quote::getDealId)
                .filter(StringUtils::hasText)
                .flatMap(dealRepo::findById)
                .map(Deal::getOwnerUsername)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    /** Trả về BigDecimal.ZERO nếu giá trị null, ngược lại trả về giá trị đó. */
    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Chuyển đổi đối tượng sang BigDecimal, trả về ZERO nếu null hoặc không hợp lệ. */
    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }
}
