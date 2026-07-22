package com.frezo.common.workflow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorkflowValidateResultDto {
    private boolean valid;
    private List<String> errors = new ArrayList<>();

    public static WorkflowValidateResultDto ok() {
        WorkflowValidateResultDto r = new WorkflowValidateResultDto();
        r.setValid(true);
        return r;
    }

    public static WorkflowValidateResultDto fail(List<String> errors) {
        WorkflowValidateResultDto r = new WorkflowValidateResultDto();
        r.setValid(false);
        r.setErrors(errors != null ? errors : List.of());
        return r;
    }
}
