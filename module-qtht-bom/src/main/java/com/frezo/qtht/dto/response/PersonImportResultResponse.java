package com.frezo.qtht.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PersonImportResultResponse {
    private int total;
    private int success;
    private int failed;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
