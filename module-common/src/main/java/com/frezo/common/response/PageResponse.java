package com.frezo.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * ThiNVQ : Typed pagination response — thay thế Map<String,Object> để đảm bảo type-safety.
 * Dùng cho tất cả API trả về danh sách phân trang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private int pageNumber;
    private int pageSize;
    private long total;
    private List<T> items;

    /**
     * ThiNVQ : Factory method — tạo PageResponse từ Spring Page + list items đã map.
     */
    public static <T> PageResponse<T> of(int pageNumber, int pageSize, Page<?> page, List<T> items) {
        return PageResponse.<T>builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .total(page.getTotalElements())
                .items(items)
                .build();
    }
}
