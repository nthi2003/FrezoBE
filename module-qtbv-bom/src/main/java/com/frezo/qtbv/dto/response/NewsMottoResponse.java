package com.frezo.qtbv.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsMottoResponse {
    private String id;
    private String content;
    private String author;
}
