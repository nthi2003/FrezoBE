package com.frezo.qtht.service;

import com.frezo.common.exception.AppException;
import com.frezo.common.response.PageResponse;
import com.frezo.qtht.common.DepartmentStatus;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.mapper.DepartmentMapper;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link DepartmentServiceImpl} — mock toàn bộ dependency, không cần Spring context.
 * <p>
 * Reference cho các service test khác. Chuẩn: Arrange - Act - Assert, 1 assertion per behavior.
 */
@DisplayName("DepartmentService — unit test")
@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PersonRepository personRepository;
    @Mock DepartmentMapper departmentMapper;

    @InjectMocks DepartmentServiceImpl service;

    private Department entity;
    private DepartmentResponse dto;

    @BeforeEach
    void setUp() {
        entity = new Department();
        entity.setId("dept-1");
        entity.setName("Phòng IT");
        entity.setCode("IT");
        entity.setLevel(1);
        entity.setStatus(DepartmentStatus.ACTIVE);
        entity.setIsDeleted(false);

        dto = new DepartmentResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
    }

    // ------------------------------------------------------------------
    // all() — pagination + PageResponse
    // ------------------------------------------------------------------

    @Test
    @DisplayName("all() trả PageResponse rỗng khi repo không có dữ liệu")
    void all_shouldReturnEmptyPageResponse_whenRepoEmpty() {
        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        filter.setPageNumber(0);
        filter.setPageSize(10);

        Page<Department> emptyPage = new PageImpl<>(List.of());
        when(departmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        PageResponse<DepartmentResponse> result = service.all(filter);

        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPageNumber()).isZero();
    }

    @Test
    @DisplayName("all() trả PageResponse có item khi repo có dữ liệu")
    void all_shouldMapEntitiesToResponses() {
        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        filter.setPageNumber(0);
        filter.setPageSize(10);

        Page<Department> page = new PageImpl<>(List.of(entity));
        when(departmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(departmentMapper.toResponse(entity)).thenReturn(dto);

        PageResponse<DepartmentResponse> result = service.all(filter);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getCode()).isEqualTo("IT");
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("all() throw INVALID_SORT_FIELD khi sortBy không trong whitelist")
    void all_shouldThrow_whenSortByNotWhitelisted() {
        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        filter.setSortBy("secretField");   // không nằm trong ALLOWED_SORT_FIELDS

        assertThatThrownBy(() -> service.all(filter))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(com.frezo.common.exception.CommonErrorCode.INVALID_SORT_FIELD);
    }

    // ------------------------------------------------------------------
    // create() — validation + save
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create() throw DEPARTMENT_CODE_EXISTS khi code đã tồn tại")
    void create_shouldThrow_whenCodeExists() {
        DepartmentSaveRequest request = new DepartmentSaveRequest();
        request.setCode("IT");
        request.setName("Phòng IT");
        when(departmentRepository.existsByCode("IT")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(QthtErrorCode.DEPARTMENT_CODE_EXISTS);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create() save entity + trả DepartmentResponse khi code chưa có")
    void create_shouldSaveAndReturnDto_whenValid() {
        DepartmentSaveRequest request = new DepartmentSaveRequest();
        request.setCode("HR");
        request.setName("Phòng nhân sự");

        when(departmentRepository.existsByCode("HR")).thenReturn(false);
        when(departmentMapper.toEntity(request)).thenReturn(entity);
        when(departmentRepository.save(entity)).thenReturn(entity);
        when(departmentMapper.toResponse(entity)).thenReturn(dto);

        DepartmentResponse result = service.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("IT");
        verify(departmentRepository).save(entity);
    }

    // ------------------------------------------------------------------
    // delete() — soft-delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("delete() soft-delete (set isDeleted=true + deletedAt + deletedBy)")
    void delete_shouldSoftDelete() {
        when(departmentRepository.findByIdAndIsDeletedFalse("dept-1")).thenReturn(Optional.of(entity));

        service.delete("dept-1");

        assertThat(entity.getIsDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
        verify(departmentRepository).save(entity);
    }

    @Test
    @DisplayName("delete() throw DEPARTMENT_NOT_FOUND khi id không tồn tại")
    void delete_shouldThrow_whenNotFound() {
        when(departmentRepository.findByIdAndIsDeletedFalse("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("bogus"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(QthtErrorCode.DEPARTMENT_NOT_FOUND);
    }
}
