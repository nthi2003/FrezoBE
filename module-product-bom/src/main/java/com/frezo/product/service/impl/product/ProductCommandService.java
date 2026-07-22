package com.frezo.product.service.impl.product;

import com.frezo.common.audit.AuditAction;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.product.dto.request.ProductCreateRequest;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.request.ProductUpdateRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.entity.Product;
import com.frezo.product.mapper.ProductMapper;
import com.frezo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD sản phẩm — tách khỏi façade để mỗi class ≤5 deps (Batch B refactor 2026-07).
 */
@Component
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public PageResponse<ProductResponse> filter(ProductFilterRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getSize() != null ? request.getSize() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Product> page = productRepository.findAll(createSpecification(request), pageable);
        List<ProductResponse> responses = page.getContent().stream().map(productMapper::toResponse).toList();
        return PageResponse.of(pageNum, pageSize, page, responses);
    }

    public ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        return productMapper.toResponse(product);
    }

    @Transactional
    @AuditAction(value = "Thêm mới sản phẩm", entity = "Product", action = "CREATE")
    public ProductResponse create(ProductCreateRequest request) {
        Product product = productMapper.toEntity(request);
        if (product.getCode() == null || product.getCode().isBlank()) {
            product.setCode(generateProductCode());
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    @AuditAction(value = "Cập nhật sản phẩm", entity = "Product", action = "UPDATE")
    public ProductResponse update(String id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    @AuditAction(value = "Xóa sản phẩm", entity = "Product", action = "DELETE")
    public void delete(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        product.setIsActive(false);
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    private String generateProductCode() {
        String maxCode = productRepository.findMaxCode();
        if (maxCode == null) return "SP001";
        int num = Integer.parseInt(maxCode.replaceAll("\\D+", "")) + 1;
        return String.format("SP%03d", num);
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
