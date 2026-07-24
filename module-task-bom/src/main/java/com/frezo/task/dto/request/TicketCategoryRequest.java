package com.frezo.task.dto.request;

import lombok.Data;

@Data
public class TicketCategoryRequest {
    private String code;
    private String name;
    private Integer sortOrder;
    private Boolean active;
}
