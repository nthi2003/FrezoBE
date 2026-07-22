package com.frezo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/**
 * Typed pagination response — thay thế {@code Map<String,Object>} để đảm bảo type-safety.
 * Dùng cho tất cả API trả về danh sách phân trang.
 * <p>
 * Xem chi tiết: {@code FrezoBE/API_DESIGN_STANDARD.md §2.3, §4}.
 * <p>
 * Ví dụ:
 * <pre>
 * // Trong service
 * Page&lt;Department&gt; page = departmentRepository.findAll(spec, pageable);
 * return PageResponse.from(page, departmentMapper::toResponse);
 *
 * // Trong controller
 * return ApiResponse.ok(departmentService.search(filter));
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    /** 0-based page number. */
    private int pageNumber;

    private int pageSize;

    /** Total elements across all pages. */
    private long total;

    /** Total pages. */
    private int totalPages;

    private boolean hasNext;

    private boolean hasPrevious;

    /** Optional — field đang sort. */
    private String sortBy;

    /** Optional — direction: "asc" hoặc "desc". */
    private String sortDir;

    private List<T> items;

    // ----------------------------------------------------------------
    // Factory — khuyến nghị dùng
    // ----------------------------------------------------------------

    /**
     * Tạo {@code PageResponse<T>} từ Spring {@link Page} — dữ liệu đã đúng type T.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .sortBy(extractSortBy(page.getSort()))
                .sortDir(extractSortDir(page.getSort()))
                .items(page.getContent())
                .build();
    }

    /**
     * Tạo {@code PageResponse<T>} từ {@code Page<E>} + mapper — thường dùng khi entity → DTO trong service.
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .sortBy(extractSortBy(page.getSort()))
                .sortDir(extractSortDir(page.getSort()))
                .items(page.getContent().stream().map(mapper).toList())
                .build();
    }

    /**
     * PageResponse rỗng — dùng khi input filter không hợp lệ / không có kết quả.
     */
    public static <T> PageResponse<T> empty(int pageNumber, int pageSize) {
        return PageResponse.<T>builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .total(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .items(List.of())
                .build();
    }

    // ----------------------------------------------------------------
    // Backward-compat factory
    // ----------------------------------------------------------------

    /**
     * @deprecated dùng {@link #from(Page)} hoặc {@link #from(Page, Function)}.
     * Factory cũ để backward-compat với code đang gọi.
     */
    @Deprecated(since = "1.1", forRemoval = false)
    public static <T> PageResponse<T> of(int pageNumber, int pageSize, Page<?> page, List<T> items) {
        return PageResponse.<T>builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .items(items)
                .build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String extractSortBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) return null;
        Sort.Order first = sort.stream().findFirst().orElse(null);
        return first != null ? first.getProperty() : null;
    }

    private static String extractSortDir(Sort sort) {
        if (sort == null || sort.isUnsorted()) return null;
        Sort.Order first = sort.stream().findFirst().orElse(null);
        return first != null ? first.getDirection().name().toLowerCase() : null;
    }
}
