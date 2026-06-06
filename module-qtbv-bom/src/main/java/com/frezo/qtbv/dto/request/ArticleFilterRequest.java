package com.frezo.qtbv.dto.request;

import com.frezo.common.model.PagingBase;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.common.PublishScope;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleFilterRequest extends PagingBase {
    private String title;
    private String code;
    private String authorId;
    private String managerId;
    private ArticleStatus status;
    private PublishScope publishScope;
    private String organizationId;
    private LocalDateTime publishDateFrom;
    private LocalDateTime publishDateTo;
    private Integer minHeart;
}
