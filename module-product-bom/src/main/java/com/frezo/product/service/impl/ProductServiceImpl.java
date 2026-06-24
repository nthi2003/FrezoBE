package com.frezo.product.service.impl;

import com.frezo.common.audit.AuditAction;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.common.service.MinioService;
import com.frezo.product.dto.request.ProductCreateRequest;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.request.ProductUpdateRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.mapper.ProductMapper;
import com.frezo.product.repository.*;
import com.frezo.product.entity.*;
import com.frezo.product.dto.request.*;
import com.frezo.product.dto.response.ProductDashboardStats;
import com.frezo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final PriceGroupRepository priceGroupRepository;
    private final PriceConfigRepository priceConfigRepository;
    private final BatchRepository batchRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final ProductMapper productMapper;
    private final MinioService minioService;

    @Override
    public PageResponse<ProductResponse> filter(ProductFilterRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getSize() != null ? request.getSize() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Product> page = productRepository.findAll(createSpecification(request), pageable);
        List<ProductResponse> responses = page.getContent().stream().map(productMapper::toResponse).toList();
        return PageResponse.of(pageNum, pageSize, page, responses);
    }

    @Override
    public ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    @AuditAction(value = "Thêm mới sản phẩm", entity = "Product", action = "CREATE")
    public ProductResponse create(ProductCreateRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @AuditAction(value = "Cập nhật sản phẩm", entity = "Product", action = "UPDATE")
    public ProductResponse update(String id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        productMapper.updateEntity(request, product);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void bulkUpdatePrices(List<PriceUpdateRequest> requests) {
        for (PriceUpdateRequest req : requests) {
            Product product = productRepository.findByCode(req.getProductCode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + req.getProductCode()));

            for (PriceUpdateRequest.UnitPrice up : req.getUnitPrices()) {
                // Find or Create ProductUnit
                ProductUnit unit = productUnitRepository.findByProductId(product.getId()).stream()
                        .filter(u -> u.getUnitName().equalsIgnoreCase(up.getUnitName()))
                        .findFirst()
                        .orElseGet(() -> {
                            ProductUnit newUnit = ProductUnit.builder()
                                    .productId(product.getId())
                                    .unitName(up.getUnitName())
                                    .conversionRate(1.0)
                                    .build();
                            return productUnitRepository.save(newUnit);
                        });

                // Find PriceGroup
                PriceGroup group = priceGroupRepository.findByCode(up.getPriceGroupCode())
                        .orElseThrow(() -> new RuntimeException("PriceGroup not found: " + up.getPriceGroupCode()));

                // Update PriceConfig
                List<PriceConfig> activeConfigs = priceConfigRepository.findActivePrice(unit.getId(), group.getId());
                PriceConfig config;
                if (!activeConfigs.isEmpty()) {
                    config = activeConfigs.get(0);
                    config.setPrice(up.getNewPrice());
                } else {
                    config = PriceConfig.builder()
                            .productUnitId(unit.getId())
                            .priceGroupId(group.getId())
                            .price(up.getNewPrice())
                            .effectiveDate(LocalDateTime.now())
                            .build();
                }
                priceConfigRepository.save(config);
            }
        }
    }

    @Override
    @Transactional
    public void importBatch(BatchImportRequest request) {
        for (BatchImportRequest.BatchItem item : request.getItems()) {
            Product product = productRepository.findByCode(item.getProductCode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductCode()));

            // Create Batch
            Batch batch = Batch.builder()
                    .productId(product.getId())
                    .supplierId(request.getSupplierId())
                    .batchCode("BATCH-" + System.currentTimeMillis() + "-" + item.getProductCode())
                    .growingArea(item.getGrowingArea())
                    .importDate(java.time.LocalDate.now())
                    .expiryDate(item.getExpiryDate())
                    .initialQuantity(item.getQuantity())
                    .currentQuantity(item.getQuantity())
                    .costPrice(item.getCostPrice())
                    .build();
            batchRepository.save(batch);

            // Log Inventory
            InventoryLog log = InventoryLog.builder()
                    .productId(product.getId())
                    .batchId(batch.getId())
                    .type("IMPORT")
                    .quantity(item.getQuantity())
                    .note("Nhập kho nhanh từ chuyến xe")
                    .build();
            inventoryLogRepository.save(log);
        }
    }

    @Override
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

    @Override
    public ProductDashboardStats getDashboardStats() {
        return ProductDashboardStats.builder()
                .totalProducts(productRepository.countActiveProducts())
                .lowStockCount(productRepository.countLowStockProducts())
                .expiringSoonCount(batchRepository.countExpiringSoon(java.time.LocalDate.now().plusDays(1)))
                .todayRevenue(0.0) // Sẽ bổ sung query doanh thu sau khi có bảng đơn hàng
                .build();
    }

    @Override
    public List<java.util.Map<String, Object>> getProfitChart(int days) {
        java.time.LocalDate fromDate = java.time.LocalDate.now().minusDays(days);
        List<Object[]> results = inventoryLogRepository.getProfitChartData(fromDate);
        List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        for (Object[] res : results) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", res[0].toString());
            map.put("cost", res[1]);
            map.put("revenue", res[2]);
            data.add(map);
        }
        return data;
    }

    @Override
    public List<Map<String, Object>> getPriceFluctuation() {
        List<Object[]> results = priceConfigRepository.getPriceFluctuation();
        List<java.util.Map<String, Object>> data = new java.util.ArrayList<>();
        for (Object[] res : results) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", res[0]);
            map.put("todayPrice", res[1]);
            map.put("yesterdayPrice", res[2]);
            
            Object changeVal = res[3];
            double change = 0.0;
            if (changeVal != null) {
                try {
                    change = Double.parseDouble(changeVal.toString());
                } catch (Exception e) {
                    log.warn("Failed to parse price change percentage: {}", changeVal);
                }
            }
            map.put("change", change);
            map.put("status", change >= 0 ? "up" : "down");
            data.add(map);
        }
        return data;
    }

    @Override
    public List<Map<String, Object>> getMarketComparison() {
        List<Object[]> results = marketPriceRepository.getTodayMarketComparison();
        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] res : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", res[0]);
            map.put("marketName", res[1]);
            map.put("marketPrice", res[2]);
            map.put("unit", res[3]);
            map.put("yourPrice", res[4] != null ? res[4] : 0.0);
            data.add(map);
        }
        return data;
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file) {
        String objectName = "products/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String url = minioService.uploadFile(objectName, file, "freo-prod");
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", file.getOriginalFilename());
        return result;
    }

    @Override
    @Transactional
    @AuditAction(value = "Xóa sản phẩm", entity = "Product", action = "DELETE")
    public void delete(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        product.setIsActive(false);
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    private Specification<Product> createSpecification(ProductFilterRequest f) {
        Specification<Product> spec = Specification.where(GenericSpecification.hasFieldIs("isDeleted", false));

        if (SystemUtils.isNotNullOrEmpty(f.getKeyword())) {
            spec = spec.and(GenericSpecification.<Product>likeField("name", f.getKeyword())
                    .or(GenericSpecification.<Product>likeField("code", f.getKeyword())));
        }

        if (SystemUtils.isNotNullOrEmpty(f.getCategoryId())) {
            spec = spec.and(GenericSpecification.<Product>equalField("categoryId", f.getCategoryId()));
        }

        if (f.getIsActive() != null) {
            spec = spec.and(GenericSpecification.<Product>equalField("isActive", f.getIsActive()));
        }

        return spec;
    }
}
