package com.frezo.qtbv.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleManagerEditRequest {
    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String newContent;
}
