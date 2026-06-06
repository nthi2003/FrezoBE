package com.frezo.task.dto.request;

import lombok.Data;

@Data
public class TagRequest {
    private String code;

    private String name;

    private String category;

    private String color;
}
