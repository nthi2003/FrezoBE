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


    private int pageNumber;

    private int pageSize;

    private long total;

    private int totalPages;

    private boolean hasNext;

    private boolean hasPrevious;


    private String sortBy;


    private String sortDir;

    private List<T> items;


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
