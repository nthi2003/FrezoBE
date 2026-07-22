package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.recruitment.RecruitmentConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/qlns/recruitment")
@RequiredArgsConstructor
@Tag(name = "Recruitment — Meta", description = "Stages / transitions mặc định")
public class RecruitmentMetaController {

    @Operation(summary = "Danh sách stage ATS mặc định + transition hợp lệ")
    @GetMapping("/stages")
    public ApiResponse<Map<String, Object>> stages() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stages", RecruitmentConstants.STAGES);
        Map<String, List<String>> transitions = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> e : RecruitmentConstants.STAGE_TRANSITIONS.entrySet()) {
            transitions.put(e.getKey(), e.getValue().stream().sorted().toList());
        }
        body.put("transitions", transitions);
        return ApiResponse.ok(body);
    }
}
