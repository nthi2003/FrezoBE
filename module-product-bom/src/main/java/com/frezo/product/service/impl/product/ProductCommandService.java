package com.frezo.product.service.impl.product;

import com.frezo.common.audit.AuditAction;
import com.frezo.common.exception.AppException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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

    @Transactional
    public ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        try {
            productRepository.incrementViewCount(id);
            product.setViewCount(product.getViewCount() == null ? 1L : product.getViewCount() + 1);
        } catch (Exception ignored) {
            // không chặn xem SP nếu tăng counter lỗi
        }
        return productMapper.toResponse(product);
    }

    @Transactional
    @AuditAction(value = "Thêm mới sản phẩm", entity = "Product", action = "CREATE")
    public ProductResponse create(ProductCreateRequest request) {
        Product product = productMapper.toEntity(request);
        String code = normalizeRequiredCode(product.getCode());
        product.setCode(code);
        if (productRepository.findByCode(code).isPresent()) {
        }
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    @AuditAction(value = "Cập nhật sản phẩm", entity = "Product", action = "UPDATE")
    public ProductResponse update(String id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("product.not.found"));
        if (request.getCode() != null) {
            String code = normalizeRequiredCode(request.getCode());
            request.setCode(code);
            productRepository.findByCode(code)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new AppException("product.code.already.exists", HttpStatus.CONFLICT, code);
                    });
        }
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

    private String normalizeRequiredCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AppException("product.code.required", HttpStatus.BAD_REQUEST);
        }
        return raw.trim();
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
