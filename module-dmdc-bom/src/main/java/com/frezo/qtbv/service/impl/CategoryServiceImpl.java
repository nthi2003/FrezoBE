package com.frezo.qtbv.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qtbv.common.DmdcErrorCode;
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

    /** Liệt kê danh mục theo bộ lọc (nhóm, từ khóa) với phân trang, sắp xếp theo orderIndex. */
    public PageResponse<CategoryResponse> all(CategoryFilter filter) {
        Specification<Category> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.ASC , "orderIndex");
        Page<Category> entities = categoryRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(),sort));
        return PageResponse.from(entities, categoryMapper::toResponse);
    }

    /** Tạo mới danh mục sau khi kiểm tra trùng mã và tên. */
    @Transactional
    public ApiResponse<?> add(CategoryRequest request) {
        validateRequest(request, null);
        Category category = categoryMapper.toEntity(request);
        category.setIsDeleted(false);
        Category save = categoryRepository.save(category);
        return ApiResponse.ok(categoryMapper.toResponse(save));

    }

    /** Cập nhật danh mục, kiểm tra trùng mã/tên với danh mục khác. */
    @Transactional
    public ApiResponse<?> edit(String id, CategoryRequest request) {
        Category category = findEntityById(id);
        validateRequest(request, id);
        categoryMapper.updateEntity(request, category);
        Category save  = categoryRepository.save(category);
        return ApiResponse.ok(categoryMapper.toResponse(save));
    }

    /** Xem chi tiết danh mục theo ID. */
    public CategoryResponse view (String id) {
        Category category = findEntityById(id);
        return  categoryMapper.toResponse(category);
    }
    /** Xóa mềm danh mục bằng cách đánh dấu isDeleted = true. */
    public void delete (String id) {
        Category category = findEntityById(id);
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    /** Kiểm tra trùng mã, tên và tên tiếng Anh khi tạo/sửa danh mục. */
    private void validateRequest(CategoryRequest request, String excludeId) {
        boolean codeExists = excludeId == null
                ? categoryRepository.existsByCodeAndIsDeletedFalse(request.getCode())
                : categoryRepository.existsByCodeAndIsDeletedFalseAndIdNot(request.getCode(), excludeId);
        if (codeExists) {
            throw new AppException(DmdcErrorCode.CATEGORY_CODE_EXISTS, request.getCode());
        }

        boolean nameExists = excludeId == null
                ? categoryRepository.existsByNameAndIsDeletedFalse(request.getName())
                : categoryRepository.existsByNameAndIsDeletedFalseAndIdNot(request.getName(), excludeId);
        if (nameExists) {
            throw new AppException(DmdcErrorCode.CATEGORY_NAME_EXISTS, request.getName());
        }

        if (request.getNameEn() != null) {
            boolean nameEnExists = excludeId == null
                    ? categoryRepository.existsByNameEnAndIsDeletedFalse(request.getNameEn())
                    : categoryRepository.existsByNameEnAndIsDeletedFalseAndIdNot(request.getNameEn(), excludeId);
            if (nameEnExists) {
                throw new AppException(DmdcErrorCode.CATEGORY_NAME_EN_EXISTS, request.getNameEn());
            }
        }
    }

    /** Tìm danh mục theo ID, ném ngoại lệ nếu không tồn tại. */
    protected Category findEntityById(String id) {

        return categoryRepository.findById(id).orElseThrow(() -> new AppException(DmdcErrorCode.ENTITY_NOT_FOUND));
    }
    /** Tạo JPA Specification từ bộ lọc danh mục (nhóm, từ khóa). */
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
