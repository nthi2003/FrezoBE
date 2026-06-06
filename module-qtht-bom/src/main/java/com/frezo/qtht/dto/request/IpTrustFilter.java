package com.frezo.qtht.dto.request;

import com.frezo.common.model.PagingBase;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpTrustFilter extends PagingBase {
    private String keyword;
}