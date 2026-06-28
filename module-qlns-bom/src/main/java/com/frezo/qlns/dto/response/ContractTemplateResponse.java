package com.frezo.qlns.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateResponse {
    private String id;
    private String name;
    private String type;
    private String fileUrl;
    private LocalDateTime createdDate;
}
