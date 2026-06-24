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
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.frezo.common.helper.GenericSpecification.likeField;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public Map<String, Object> all (CategoryFilter filter) {
        Specification<Category> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.ASC , "orderIndex");
        Page<Category> entities = categoryRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(),sort));
        List<CategoryResponse> responses = entities.getContent().stream()
                .map(categoryMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities , responses);
    }

    @Transactional
    public Response<?> add(CategoryRequest request) {
        validateRequest(request);
        Category category = categoryMapper.toEntity(request);
        category.setIsDeleted(false);
        Category save = categoryRepository.save(category);
        return Response.ok(categoryMapper.toResponse(save));

    }

    @Transactional
    public Response<?> edit(String id, CategoryRequest request) {
        Category category = findEntityById(id);
        validateRequest(request);
        categoryMapper.updateEntity(request, category);
        Category save  = categoryRepository.save(category);
        return Response.ok(categoryMapper.toResponse(save));
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

    private void validateRequest(CategoryRequest request) {

        if (categoryRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new QTHTException("code.exist");
        } else if (categoryRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new QTHTException("name.exist");
        } else if (categoryRepository.existsByNameEnAndIsDeletedFalse(request.getNameEn())) {
            throw new QTHTException("name.en.exist");
        }

    }

    protected Category findEntityById(String id) {

        return categoryRepository.findById(id).orElseThrow(() -> new QTHTException("valid.not.found"));
    }
    private Specification<Category> createSpecification (CategoryFilter filter) {
        Specification<Category> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted" , Boolean.FALSE));
        
        if (SystemUtils.isNotNullOrEmpty(filter.getType())) {
            specification = specification.and(GenericSpecification.equalField("groupCode", filter.getType()));
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
