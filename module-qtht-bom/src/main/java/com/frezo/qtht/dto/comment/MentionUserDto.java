package com.frezo.qtht.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionUserDto {
    private String id;
    private String username;
    private String fullName;
    private String avatar;
    private String email;
}
