package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.frezo.qtht.common.OrganizationScale;
import com.frezo.qtht.common.OrganizationStatus;
import com.frezo.qtht.common.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationAddRequest {
    @NotBlank(message = "Mã tổ chức không được để trống")
    @Size(max = 50, message = "Mã tổ chức không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã tổ chức chỉ được chứa chữ hoa, số và dấu gạch dưới")
    private String code;

    @JsonProperty("taxCode")
    @Size(max = 20, message = "Mã số thuế không được vượt quá 20 ký tự")
    private String taxCode;

    @NotBlank(message = "Tên tổ chức không được để trống")
    @Size(max = 255, message = "Tên tổ chức không được vượt quá 255 ký tự")
    private String name;

    @JsonProperty("shortName")
    @Size(max = 100, message = "Tên viết tắt không được vượt quá 100 ký tự")
    private String shortName;

    @JsonProperty("nameEn")
    @Size(max = 255, message = "Tên tiếng Anh không được vượt quá 255 ký tự")
    private String nameEn;

    private String description;

    @Size(max = 500, message = "URL logo không được vượt quá 500 ký tự")
    private String logoUrl;

    @Size(max = 200, message = "Website không được vượt quá 200 ký tự")
    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$",
        message = "Website không đúng định dạng")
    private String website;

    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Email không đúng định dạng")
    private String email;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Số điện thoại chỉ được chứa số và các ký tự +, -, (, )")
    private String phone;

    @Size(max = 20, message = "Số fax không được vượt quá 20 ký tự")
    private String fax;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String address;

    @JsonProperty("provinceCode")
    @Size(max = 20, message = "Mã tỉnh không được vượt quá 20 ký tự")
    private String provinceCode;

    @JsonProperty("wardCode")
    @Size(max = 20, message = "Mã phường/xã không được vượt quá 20 ký tự")
    private String wardCode;

    // Thông tin phân cấp
    @JsonProperty("parentId")
    private String parentId;

    @NotNull(message = "Cấp độ không được để trống")
    private Integer level;

    private Integer orderIndex;

    // Phân loại tổ chức
    @NotNull(message = "Loại tổ chức không được để trống")
    private OrganizationType type;

    @JsonProperty("scale")
    private OrganizationScale scale;

    @Size(max = 200, message = "Lĩnh vực hoạt động không được vượt quá 200 ký tự")
    private String businessSector;

    // Thời gian
    private LocalDateTime establishedDate;

    // Trạng thái
    @NotNull(message = "Trạng thái không được để trống")
    private OrganizationStatus status;

    // Đại diện pháp luật
    private String legalRepresentativeId;


    // Thông tin liên hệ
    @JsonProperty("contactPerson")
    @Size(max = 100, message = "Tên người liên hệ không được vượt quá 100 ký tự")
    private String contactPerson;

    @JsonProperty("contactPosition")
    @Size(max = 100, message = "Chức vụ người liên hệ không được vượt quá 100 ký tự")
    private String contactPosition;

    @JsonProperty("contactEmail")
    @Size(max = 100, message = "Email người liên hệ không được vượt quá 100 ký tự")
    @Pattern(regexp = "^$|^[A-Za-z0-9+_.-]+@(.+)$", message = "Email người liên hệ không đúng định dạng")
    private String contactEmail;

    @JsonProperty("contactPhone")
    @Size(max = 20, message = "Số điện thoại người liên hệ không được vượt quá 20 ký tự")
    @Pattern(regexp = "^$|^[0-9+\\-() ]*$", message = "Số điện thoại người liên hệ chỉ được chứa số và các ký tự +, -, (, )")
    private String contactPhone;
}
