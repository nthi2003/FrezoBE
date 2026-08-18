package com.frezo.product.service.impl.product;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.product.dto.request.PriceUpdateRequest;
import com.frezo.product.dto.request.ProductSalePriceUpdateRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.dto.response.ProductSalePriceHistoryResponse;
import com.frezo.product.entity.PriceConfig;
import com.frezo.product.entity.PriceGroup;
import com.frezo.product.entity.Product;
import com.frezo.product.entity.ProductSalePriceHistory;
import com.frezo.product.entity.ProductUnit;
import com.frezo.product.mapper.ProductMapper;
import com.frezo.product.repository.PriceConfigRepository;
import com.frezo.product.repository.PriceGroupRepository;
import com.frezo.product.repository.ProductRepository;
import com.frezo.product.repository.ProductSalePriceHistoryRepository;
import com.frezo.product.repository.ProductUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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
    private final ProductSalePriceHistoryRepository salePriceHistoryRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

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

    @Transactional
    public ProductResponse updateSalePrice(String productId, ProductSalePriceUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        Double oldPrice = product.getPrice();
        Double newPrice = request.getPrice();
        if (newPrice == null || newPrice < 0) {
            throw new AppException("product.price.invalid", HttpStatus.BAD_REQUEST);
        }
        if (Objects.equals(oldPrice, newPrice)) {
            return productMapper.toResponse(product);
        }
        product.setPrice(newPrice);
        productRepository.save(product);
        recordSalePriceChange(productId, oldPrice, newPrice, request.getNote());
        return productMapper.toResponse(product);
    }

    public List<ProductSalePriceHistoryResponse> getSalePriceHistory(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("product.not.found");
        }
        return salePriceHistoryRepository.findByProductIdAndIsDeletedFalseOrderByCreatedDateDesc(productId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public void recordSalePriceChangeIfDifferent(String productId, Double oldPrice, Double newPrice, String note) {
        if (newPrice == null || Objects.equals(oldPrice, newPrice)) {
            return;
        }
        recordSalePriceChange(productId, oldPrice, newPrice, note);
    }

    private void recordSalePriceChange(String productId, Double oldPrice, Double newPrice, String note) {
        ProductSalePriceHistory history = ProductSalePriceHistory.builder()
                .productId(productId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .note(note)
                .build();
        salePriceHistoryRepository.save(history);
    }

    private ProductSalePriceHistoryResponse toHistoryResponse(ProductSalePriceHistory history) {
        return ProductSalePriceHistoryResponse.builder()
                .id(history.getId())
                .productId(history.getProductId())
                .oldPrice(history.getOldPrice())
                .newPrice(history.getNewPrice())
                .note(history.getNote())
                .updatedBy(resolveUpdatedBy(history.getCreatedBy()))
                .updatedAt(history.getCreatedDate())
                .build();
    }

    /** Bản ghi cũ có thể lưu user UUID trong created_by — resolve sang login name nếu có. */
    private String resolveUpdatedBy(String createdBy) {
        if (createdBy == null || createdBy.isBlank() || !UUID_PATTERN.matcher(createdBy).matches()) {
            return createdBy;
        }
        return userRepository.findById(createdBy)
                .map(User::getUserName)
                .orElse(createdBy);
    }
}
