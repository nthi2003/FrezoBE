package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.common.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleReactionRequest {
    @NotNull(message = "Loại tương tác không được để trống")
    private ReactionType type;
}
