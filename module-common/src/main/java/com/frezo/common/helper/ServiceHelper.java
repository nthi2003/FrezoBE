package com.frezo.common.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @deprecated Từ v1.1, dùng {@link com.frezo.common.response.PageResponse#from(Page)} để có typed pagination response.
     * <p>Trả {@code Map<String,Object>} mất type-safety, buộc controller phải unchecked cast, không autocomplete trong FE.
     * <p>Migration:
     * <pre>
     * // Cũ
     * List&lt;DepartmentResponse&gt; items = ...;
     * return ServiceHelper.createResponse1(page, size, entityPage, items);   // Map&lt;String,Object&gt;
     *
     * // Mới
     * Page&lt;DepartmentResponse&gt; page = entityPage.map(mapper::toResponse);
     * return PageResponse.from(page);                                       // PageResponse&lt;DepartmentResponse&gt;
     * </pre>
     */
    @Deprecated(since = "1.1", forRemoval = true)
    public static <R> Map<String, Object> createResponse1(
            Integer pageNumber, Integer pageSize, Page<?> entities, List<R> items) {
        Map<String, Object> data = new HashMap<>();
        data.put("pageNumber", pageNumber);
        data.put("current", pageNumber + 1);
        data.put("pageSize", pageSize);
        data.put("total", entities.getTotalElements());
        data.put("items", items);
        return data;

    }

    public static Pageable createPageable(Integer page, Integer size, Sort sort) {
        return createPageable(page, size, sort, false);
    }

    public static Pageable createPageable(Integer page, Integer size, Sort sort, boolean enforceMax10) {
        // ThiNVQ : Ép buộc số lượng bản ghi tối đa là 10 nếu enforceMax10=true (áp dụng cho config/setting)
        if (page == null) page = 1;
        if (size == null) size = enforceMax10 ? 10 : Integer.MAX_VALUE;
        if (enforceMax10 && size > 10) size = 10;
        
        return PageRequest.of(page == 0 ? 0 : page - 1, size, sort);
    }

}
