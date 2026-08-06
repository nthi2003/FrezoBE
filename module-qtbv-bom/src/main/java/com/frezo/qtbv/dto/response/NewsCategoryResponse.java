package com.frezo.qtbv.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCategoryResponse {
    private String id;
    private String name;
    private String color;
    private String organizationId;
    private Integer orderIndex;
}
