package com.frezo.qtbv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ManagerResponse {
    private String id;
    private String code;
    private String name;
    private String email;
    private String phone;
    private String jobTitle;
}
