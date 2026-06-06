package com.frezo.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagingBase {
    private Integer pageNumber;
    private Integer pageSize;

    public Integer getPageNumber() {
        return pageNumber == null ? 0 : pageNumber;
    }
    public Integer getPageSize() {
        return  pageSize == null ? 10 : pageSize;
    }

    public Pageable toPageable() {
        return PageRequest.of(getPageNumber(), getPageSize());
    }
}
