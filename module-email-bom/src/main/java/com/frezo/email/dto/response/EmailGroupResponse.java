package com.frezo.email.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailGroupResponse {
    private String id;
    private String name;
    private String description;
    private List<String> emails;
}
