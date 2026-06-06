package com.frezo.common.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOcrAnalyzeResponse {

    private String recordId;
    private String sourceFileName;
    private String reviewStatus;
    private AiOcrDocumentDataDto analysis;
}