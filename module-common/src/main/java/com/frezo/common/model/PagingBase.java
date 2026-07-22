package com.frezo.common.model;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Base cho MỌI DTO filter list — kế thừa để có sẵn pagination + sort.
 * <p>
 * Xem chi tiết: {@code FrezoBE/API_DESIGN_STANDARD.md §4 — Pagination, Filter, Sort}.
 * <p>
 * <b>Rule quan trọng:</b> {@code sortBy} phải được whitelist trong service để chống SQL injection.
 * Dùng {@link #toPageable(Set)} với whitelist thay vì {@link #toPageable()} không whitelist.
 * <p>
 * Ví dụ:
 * <pre>
 * public class DepartmentFilterRequest extends PagingBase {
 *     private String keyword;
 *     private DepartmentStatus status;
 * }
 *
 * // Trong service
 * private static final Set&lt;String&gt; ALLOWED_SORT = Set.of("createdDate", "name", "code", "status");
 *
 * Pageable pageable = filter.toPageable(ALLOWED_SORT);   // throw AppException nếu sortBy sai
 * </pre>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagingBase {

    @Min(value = 0, message = "{validation.paging.page.min}")
    private Integer pageNumber;

    @Min(value = 1, message = "{validation.paging.size.min}")
    @Max(value = 100, message = "{validation.paging.size.max}")
    private Integer pageSize;

    /** Field name để sort — camelCase, service PHẢI whitelist. */
    private String sortBy;

    /** "asc" | "desc" — case-insensitive. Default "desc". */
    @Pattern(regexp = "(?i)asc|desc", message = "{validation.paging.sortdir.invalid}")
    private String sortDir;

    public Integer getPageNumber() {
        return pageNumber == null ? 0 : pageNumber;
    }

    public Integer getPageSize() {
        return pageSize == null ? 20 : pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortDir() {
        return sortDir == null ? "desc" : sortDir;
    }

    /**
     * @deprecated không whitelist sort field — dễ SQL injection nếu client truyền {@code sortBy} tuỳ ý.
     * Dùng {@link #toPageable(Set)} hoặc {@link #toPageable(Set, String)}.
     */
    @Deprecated(since = "1.1", forRemoval = false)
    public Pageable toPageable() {
        return PageRequest.of(getPageNumber(), getPageSize());
    }

    /**
     * Tạo Pageable với sort whitelist. Nếu {@code sortBy} không trong whitelist → throw {@link AppException}
     * với {@link CommonErrorCode#INVALID_SORT_FIELD}.
     * Nếu {@code sortBy} rỗng → sort mặc định theo {@code createdDate desc} (fallback).
     *
     * @param allowedSortFields whitelist các field cho phép sort (VD: {@code Set.of("createdDate","name","code")})
     */
    public Pageable toPageable(Set<String> allowedSortFields) {
        return toPageable(allowedSortFields, "createdDate");
    }

    /**
     * Tạo Pageable với sort whitelist + fallback field.
     *
     * @param allowedSortFields whitelist các field cho phép sort
     * @param defaultSortField field dùng khi {@code sortBy} rỗng — phải nằm trong whitelist
     */
    public Pageable toPageable(Set<String> allowedSortFields, String defaultSortField) {
        String effectiveField = (sortBy == null || sortBy.isBlank()) ? defaultSortField : sortBy;
        if (!allowedSortFields.contains(effectiveField)) {
            throw new AppException(CommonErrorCode.INVALID_SORT_FIELD, effectiveField);
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(getPageNumber(), getPageSize(), Sort.by(direction, effectiveField));
    }
}
