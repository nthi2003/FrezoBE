package com.frezo.accounting.dto.request;

import com.frezo.accounting.common.PostingSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class JournalEntryRequest {

    @NotNull
    private LocalDate postingDate;

    private LocalDate documentDate;

    @NotNull
    @Size(min = 1, max = 500)
    private String description;

    /** Mặc định MANUAL nếu client không truyền. */
    private PostingSource sourceType;

    private String sourceId;

    /** Idempotency key (server-side chống double-post). */
    private String idempotencyKey;

    @NotEmpty
    @Valid
    @Size(min = 2, message = "Chứng từ phải có ít nhất 2 dòng bút toán")
    private List<JournalLineRequest> lines;
}
