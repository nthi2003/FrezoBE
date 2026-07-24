package com.frezo.task.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketCategoryResponse {
    private String id;
    private String code;
    private String name;
    private Integer sortOrder;
    private Boolean active;
}
