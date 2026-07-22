package com.frezo.product.service.impl.product;

import com.frezo.product.dto.request.PriceUpdateRequest;
import com.frezo.product.entity.PriceConfig;
import com.frezo.product.entity.PriceGroup;
import com.frezo.product.entity.Product;
import com.frezo.product.entity.ProductUnit;
import com.frezo.product.repository.PriceConfigRepository;
import com.frezo.product.repository.PriceGroupRepository;
import com.frezo.product.repository.ProductRepository;
import com.frezo.product.repository.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bulk update giá + tính giá cho một tổ hợp product/unit/priceGroup.
 * Tách riêng để giữ façade {@code ProductServiceImpl} ≤ 5 deps.
 */
@Component
@RequiredArgsConstructor
public class ProductPricingService {

    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final PriceGroupRepository priceGroupRepository;
    private final PriceConfigRepository priceConfigRepository;

    @Transactional
    public void bulkUpdatePrices(List<PriceUpdateRequest> requests) {
        for (PriceUpdateRequest req : requests) {
            Product product = productRepository.findByCode(req.getProductCode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + req.getProductCode()));

            for (PriceUpdateRequest.UnitPrice up : req.getUnitPrices()) {
                ProductUnit unit = resolveOrCreateUnit(product.getId(), up.getUnitName());
                PriceGroup group = priceGroupRepository.findByCode(up.getPriceGroupCode())
                        .orElseThrow(() -> new RuntimeException("PriceGroup not found: " + up.getPriceGroupCode()));
                upsertActivePrice(unit.getId(), group.getId(), up.getNewPrice());
            }
        }
    }

    public Double calculatePrice(String productCode, String unitName, String priceGroupCode) {
        Product product = productRepository.findByCode(productCode)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        ProductUnit unit = productUnitRepository.findByProductId(product.getId()).stream()
                .filter(u -> u.getUnitName().equalsIgnoreCase(unitName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        PriceGroup group = priceGroupRepository.findByCode(priceGroupCode)
                .orElseThrow(() -> new RuntimeException("Price Group not found"));
        List<PriceConfig> configs = priceConfigRepository.findActivePrice(unit.getId(), group.getId());
        return configs.isEmpty() ? null : configs.get(0).getPrice();
    }

    private ProductUnit resolveOrCreateUnit(String productId, String unitName) {
        return productUnitRepository.findByProductId(productId).stream()
                .filter(u -> u.getUnitName().equalsIgnoreCase(unitName))
                .findFirst()
                .orElseGet(() -> productUnitRepository.save(ProductUnit.builder()
                        .productId(productId)
                        .unitName(unitName)
                        .conversionRate(1.0)
                        .build()));
    }

    private void upsertActivePrice(String productUnitId, String priceGroupId, Double newPrice) {
        List<PriceConfig> active = priceConfigRepository.findActivePrice(productUnitId, priceGroupId);
        PriceConfig config;
        if (!active.isEmpty()) {
            config = active.get(0);
            config.setPrice(newPrice);
        } else {
            config = PriceConfig.builder()
                    .productUnitId(productUnitId)
                    .priceGroupId(priceGroupId)
                    .price(newPrice)
                    .effectiveDate(LocalDateTime.now())
                    .build();
        }
        priceConfigRepository.save(config);
    }
}
