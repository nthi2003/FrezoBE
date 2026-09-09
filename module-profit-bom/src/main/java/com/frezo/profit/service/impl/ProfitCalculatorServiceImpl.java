package com.frezo.profit.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.profit.dto.request.ProfitCalculateRequest;
import com.frezo.profit.dto.response.ProfitCalculateResponse;
import com.frezo.profit.dto.response.ProfitSettingDto;
import com.frezo.profit.service.ProfitCalculatorService;
import com.frezo.profit.service.ProfitSettingService;
import com.frezo.product.entity.Batch;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.BatchRepository;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.entity.StockLedger;
import com.frezo.warehouse.repository.StockLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfitCalculatorServiceImpl implements ProfitCalculatorService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final ProfitSettingService profitSettingService;

    /** Tính toán lợi nhuận sản phẩm: doanh thu, giá vốn, chi phí rủi ro, nhân công, cố định, lãi ròng. */
    @Override
    @Transactional(readOnly = true)
    public ProfitCalculateResponse calculate(ProfitCalculateRequest req) {
        Product product = productRepository.findById(req.getProductId())
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm"));

        double qty = req.getQuantity();
        double salePrice = req.getSalePrice() != null ? req.getSalePrice()
                : (product.getPrice() != null ? product.getPrice() : 0d);

        CostSnapshot cost = resolveUnitCost(product.getId());
        ProfitSettingDto settings = profitSettingService.resolveForProduct(product.getId());

        double revenue = salePrice * qty;
        double cogs = cost.unitCost() * qty;
        double grossProfit = revenue - cogs;
        double riskCost = revenue * safePct(settings.getRiskCostPct()) / 100d;
        double laborCost = safe(settings.getLaborCostPerUnit()) * qty;

        int months = req.getAllocationMonths() != null && req.getAllocationMonths() > 0
                ? req.getAllocationMonths() : 1;
        double monthlyOverhead = safe(settings.getFixedMonthlyOverhead()) * months;
        double targetUnits = safe(settings.getMonthlySalesTargetUnits());
        if (targetUnits <= 0) {
            targetUnits = 1000d;
        }
        double fixedCostAllocated = (monthlyOverhead / targetUnits) * qty;

        double netProfit = grossProfit - riskCost - laborCost - fixedCostAllocated;
        double netMarginPct = revenue > 0 ? (netProfit / revenue) * 100d : 0d;

        return ProfitCalculateResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productCode(product.getCode())
                .quantity(qty)
                .salePrice(salePrice)
                .unitCost(cost.unitCost())
                .costSource(cost.source())
                .settingsScope(settings.getScope())
                .revenue(revenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .riskCost(riskCost)
                .laborCost(laborCost)
                .fixedCostAllocated(fixedCostAllocated)
                .netProfit(netProfit)
                .netProfitMarginPct(netMarginPct)
                .targetProfitMarginPct(settings.getProfitMarginPct())
                .build();
    }

    /** Xác định giá vốn đơn vị: ưu tiên từ sổ kho (GRN/GIN), fallback từ lô nhập. */
    private CostSnapshot resolveUnitCost(String productId) {
        List<StockLedger> inbound = stockLedgerRepository.findByProductId(productId).stream()
                .filter(l -> l.getUnitCost() != null && l.getUnitCost() > 0)
                .filter(l -> l.getIsDeleted() == null || !l.getIsDeleted())
                .sorted(Comparator.comparing(StockLedger::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (!inbound.isEmpty()) {
            return new CostSnapshot(inbound.getFirst().getUnitCost(), "GRN/GIN ledger");
        }

        Optional<Batch> latestBatch = batchRepository.findByProductIdAndIsDeletedFalseOrderByImportDateAsc(productId).stream()
                .filter(b -> b.getCostPrice() != null && b.getCostPrice() > 0)
                .filter(b -> b.getIsDeleted() == null || !b.getIsDeleted())
                .max(Comparator.comparing(Batch::getImportDate, Comparator.nullsLast(Comparator.naturalOrder())));
        if (latestBatch.isPresent()) {
            return new CostSnapshot(latestBatch.get().getCostPrice(), "Lô nhập nhanh");
        }

        return new CostSnapshot(0d, "Chưa có giá vốn");
    }

    /** Trả về giá trị Double an toàn, trả 0 nếu null. */
    private static double safe(Double v) {
        return v != null ? v : 0d;
    }

    /** Trả về giá trị phần trăm an toàn, trả 0 nếu null. */
    private static double safePct(Double v) {
        return v != null ? v : 0d;
    }

    private record CostSnapshot(double unitCost, String source) {}
}
