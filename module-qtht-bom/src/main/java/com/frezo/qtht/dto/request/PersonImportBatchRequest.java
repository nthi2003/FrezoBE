package com.frezo.qtht.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PersonImportBatchRequest {
    private List<PersonImportRowRequest> rows;
}
