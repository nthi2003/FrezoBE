package com.frezo.common.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConfig {
    private String module;
    private List<String> steps; // e.g. ["MANAGER", "HR"]
}
