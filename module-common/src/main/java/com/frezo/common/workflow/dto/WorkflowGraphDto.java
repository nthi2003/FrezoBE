package com.frezo.common.workflow.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Graph visual cho React Flow — khớp FE {@code WorkflowGraphDto}.
 * Lưu DB dưới dạng JSON string trong {@code WorkflowDefinition.graphJson}.
 */
@Data
public class WorkflowGraphDto {
    private Integer version = 1;
    private List<WorkflowSwimlaneDto> lanes = new ArrayList<>();
    private List<WorkflowGraphNodeDto> nodes = new ArrayList<>();
    private List<WorkflowGraphEdgeDto> edges = new ArrayList<>();

    @Data
    public static class WorkflowSwimlaneDto {
        private String id;
        private String label;
        private Integer order;
    }

    @Data
    public static class WorkflowGraphNodeDto {
        private String id;
        /** START | ACTION | DECISION | APPROVAL | END */
        private String type;
        private String label;
        private String laneId;
        private PositionDto position;
        private Map<String, Object> data;
    }

    @Data
    public static class WorkflowGraphEdgeDto {
        private String id;
        private String source;
        private String target;
        private String label;
    }

    @Data
    public static class PositionDto {
        private double x;
        private double y;

        public PositionDto() {}

        public PositionDto(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
