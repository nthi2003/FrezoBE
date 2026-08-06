package com.frezo.qtbv.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticlePinRequest {
    private String articleId;
    private String organizationId;
    private Integer sortOrder;
}
