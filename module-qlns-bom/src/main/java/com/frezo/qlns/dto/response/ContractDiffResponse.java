package com.frezo.qlns.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ContractDiffResponse {
    private Integer fromVersion;
    private Integer toVersion;
    private String fromCreatedBy;
    private String toCreatedBy;
    private LocalDateTime fromCreatedDate;
    private LocalDateTime toCreatedDate;
    private String fromDescription;
    private String toDescription;

    // Danh sách các field đã thay đổi với giá trị trước và sau
    private List<FieldChange> changedFields;

    @Getter
    @Setter
    public static class FieldChange {
        private String fieldName;     // Tên field
        private String fieldLabel;    // Nhãn tiếng Việt để hiển thị
        private String oldValue;      // Giá trị cũ (string)
        private String newValue;      // Giá trị mới (string)

        public FieldChange(String fieldName, String fieldLabel, String oldValue, String newValue) {
            this.fieldName = fieldName;
            this.fieldLabel = fieldLabel;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
