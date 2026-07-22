package com.frezo.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Page shape khớp FE TanStack / Sprint-1 mocks:
 * {@code { content, totalElements, totalPages, number, size }}.
 * Không thay {@link PageResponse} (items/total) để tránh phá API cũ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FePage<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;

    public static <T> FePage<T> of(List<T> content, int page, int size) {
        int safeSize = size <= 0 ? 20 : size;
        int safePage = Math.max(page, 0);
        long total = content.size();
        int from = Math.min(safePage * safeSize, content.size());
        int to = Math.min(from + safeSize, content.size());
        List<T> slice = content.subList(from, to);
        int pages = (int) Math.max(1, (total + safeSize - 1) / safeSize);
        return FePage.<T>builder()
                .content(slice)
                .totalElements(total)
                .totalPages(pages)
                .number(safePage)
                .size(safeSize)
                .build();
    }

    public static <T> FePage<T> all(List<T> content) {
        return FePage.<T>builder()
                .content(content)
                .totalElements(content.size())
                .totalPages(1)
                .number(0)
                .size(content.isEmpty() ? 20 : content.size())
                .build();
    }
}
