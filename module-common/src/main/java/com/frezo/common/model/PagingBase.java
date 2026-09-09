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


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagingBase {

    @Min(value = 0, message = "{validation.paging.page.min}")
    private Integer pageNumber;

    @Min(value = 1, message = "{validation.paging.size.min}")
    @Max(value = 100, message = "{validation.paging.size.max}")
    private Integer pageSize;


    private String sortBy;


    @Pattern(regexp = "(?i)asc|desc", message = "{validation.paging.sortdir.invalid}")
    private String sortDir;

    public Integer getPageNumber() {
        return pageNumber == null ? 0 : pageNumber;
    }

    public Integer getPageSize() {
        return pageSize == null ? 20 : pageSize;
    }

    public String getSortDir() {
        return sortDir == null ? "desc" : sortDir;
    }

    @Deprecated(since = "1.1", forRemoval = false)
    public Pageable toPageable() {
        return PageRequest.of(getPageNumber(), getPageSize());
    }


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
