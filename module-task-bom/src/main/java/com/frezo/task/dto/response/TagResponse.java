package com.frezo.task.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TagResponse {

    private String id;

    private String code;

    private String name;

    private String category;

    private String color;
}
