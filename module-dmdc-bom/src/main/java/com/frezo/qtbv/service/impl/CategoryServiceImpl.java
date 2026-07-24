package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.dto.response.CategoryResponse;
import com.frezo.qtbv.entity.Category;
import com.frezo.qtbv.mapper.CategoryMapper;
import com.frezo.qtbv.repository.CategoryRepository;
import com.frezo.qtbv.service.CategoryService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.frezo.common.helper.GenericSpecification.likeField;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public PageResponse<CategoryResponse> all(CategoryFilter filter) {
        Specification<Category> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.ASC , "orderIndex");
        Page<Category> entities = categoryRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(),sort));
        return PageResponse.from(entities, categoryMapper::toResponse);
    }

    @Transactional
    public ApiResponse<?> add(CategoryRequest request) {
        validateRequest(request, null);
        Category category = categoryMapper.toEntity(request);
        category.setIsDeleted(false);
        Category save = categoryRepository.save(category);
        return ApiResponse.ok(categoryMapper.toResponse(save));

    }

    @Transactional
    public ApiResponse<?> edit(String id, CategoryRequest request) {
        Category category = findEntityById(id);
        validateRequest(request, id);
        categoryMapper.updateEntity(request, category);
        Category save  = categoryRepository.save(category);
        return ApiResponse.ok(categoryMapper.toResponse(save));
    }

    public CategoryResponse view (String id) {
        Category category = findEntityById(id);
        return  categoryMapper.toResponse(category);
    }
    public void delete (String id) {
        Category category = findEntityById(id);
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    private void validateRequest(CategoryRequest request, String excludeId) {
        boolean codeExists = excludeId == null
                ? categoryRepository.existsByCodeAndIsDeletedFalse(request.getCode())
                : categoryRepository.existsByCodeAndIsDeletedFalseAndIdNot(request.getCode(), excludeId);
        if (codeExists) {
            throw new QTHTException("category.code.exist", request.getCode());
        }

        boolean nameExists = excludeId == null
                ? categoryRepository.existsByNameAndIsDeletedFalse(request.getName())
                : categoryRepository.existsByNameAndIsDeletedFalseAndIdNot(request.getName(), excludeId);
        if (nameExists) {
            throw new QTHTException("category.name.exist", request.getName());
        }

        if (request.getNameEn() != null) {
            boolean nameEnExists = excludeId == null
                    ? categoryRepository.existsByNameEnAndIsDeletedFalse(request.getNameEn())
                    : categoryRepository.existsByNameEnAndIsDeletedFalseAndIdNot(request.getNameEn(), excludeId);
            if (nameEnExists) {
                throw new QTHTException("category.name.en.exist", request.getNameEn());
            }
        }
    }

    protected Category findEntityById(String id) {

        return categoryRepository.findById(id).orElseThrow(() -> new QTHTException("valid.not.found"));
    }
    private Specification<Category> createSpecification (CategoryFilter filter) {
        Specification<Category> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted" , Boolean.FALSE));
        
        String groupCode = SystemUtils.isNotNullOrEmpty(filter.getGroupCode()) ? filter.getGroupCode() : filter.getType();
        if (SystemUtils.isNotNullOrEmpty(groupCode)) {
            specification = specification.and(GenericSpecification.equalField("groupCode", groupCode));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Category>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("code", filter.getKeyword()))
                            .or(GenericSpecification.likeField("nameEn", filter.getKeyword())));

        }
        return specification;

    }


}
