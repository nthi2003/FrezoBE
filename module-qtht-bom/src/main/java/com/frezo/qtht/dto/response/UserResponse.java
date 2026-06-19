package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String address;
    private String jobTitle;
    private Boolean activated;
    private Boolean isAdmin;
    private Short dataAction; // 1=Nội bộ, 2=Cấp cha con, 3=Toàn quyền
    private Integer status; // 1=Active, 0=Inactive
    private String personId;
    private String orgId;
    private String orgName;
    private String departmentId;
    private String departmentName;
    private List<String> roles; // Danh sách role codes
    private List<String> roleNames; // Danh sách role names
}

