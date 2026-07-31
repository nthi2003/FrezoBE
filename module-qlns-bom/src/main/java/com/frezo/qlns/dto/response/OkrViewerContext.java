package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OkrViewerContext {
    private String personId;
    private boolean admin;
    private boolean manager;
    /** Scopes caller may request: mine, team, all */
    private List<String> allowedScopes;
}
