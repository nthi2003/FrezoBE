package com.frezo.email.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class EmailConfigFilter  extends PagingBase {
    private String keyword;
    public String name;
    public String apiKey;
    private String smtp;
    private Short port;
    private String nameEmail;
}
