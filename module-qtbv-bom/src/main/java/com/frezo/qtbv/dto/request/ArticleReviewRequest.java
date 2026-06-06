package com.frezo.qtbv.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleReviewRequest {
    @NotNull(message = "Trang thái duyệt không được để trống")
    private Boolean approved;

    private String note;
}
