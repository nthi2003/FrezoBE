package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public final class OkrWorkflowRequests {
    private OkrWorkflowRequests() {}

    @Data
    public static class Cycle {
        private String name;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class TimelineStep {
        private String stepName;
        private String departmentName;
        private String timeLabel;
        private String detail;
        private String result;
        private Integer sortOrder;
    }

    @Data
    public static class FeedbackType {
        private String name;
    }

    @Data
    public static class Feedback {
        private String objectiveId;
        private String targetScope;
        private String targetDepartmentId;
        private String feedbackTypeId;
        private String content;
    }

    @Data
    public static class Action {
        private String title;
        private String planUrl;
        private LocalDate startDate;
        private LocalDate endDate;
        private String result;
        private String status;
        private List<String> relatedPersonIds;
    }

    @Data
    public static class CheckIn {
        private String progress;
        private String delayedWork;
        private String blockers;
        private String solutions;
        private Integer confidenceLevel;
        private String managerPersonId;
        private String officialUpdate;
        private String managerFeedback;
        private LocalDate nextCheckInDate;
        private Boolean completeOkrs;
    }

    @Data
    public static class CheckInFeedback {
        private String parentFeedbackId;
        private String content;
    }
}
