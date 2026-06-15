package com.frezo.qtht.dto.request;

import com.frezo.common.model.PagingBase;
import jakarta.persistence.Entity;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonFilterRequest extends PagingBase {
    private String keyword;
    private Boolean activated;
    private String gender;
    private LocalDate dob;
    private String jobTitle;
    private Boolean isAdmin;
    private String departmentId;
    private String orgId;

}
