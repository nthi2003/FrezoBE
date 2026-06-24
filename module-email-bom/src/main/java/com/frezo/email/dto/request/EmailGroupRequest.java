package com.frezo.email.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class EmailGroupRequest {
    private String name;
    private String description;
    private List<String> emails;
}
