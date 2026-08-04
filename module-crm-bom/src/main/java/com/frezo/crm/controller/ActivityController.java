package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.dto.ActivityRequest;
import com.frezo.crm.service.ActivityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crm/activities")
@RequiredArgsConstructor
@Tag(name = "CRM - Activities Timeline")
public class ActivityController {

    private final ActivityService svc;

    @GetMapping
    @CheckPermission(api = "/crm/activities", action = "VIEW")
    public ApiResponse<?> list(@RequestParam(required = false) String dealId,
                               @RequestParam(required = false) String customerId) {
        if (dealId != null) return ApiResponse.ok(svc.byDeal(dealId));
        if (customerId != null) return ApiResponse.ok(svc.byCustomer(customerId));
        return ApiResponse.ok(java.util.List.of());
    }

    @PostMapping
    @CheckPermission(api = "/crm/activities", action = "CREATE")
    public ApiResponse<?> log(@RequestBody @Valid ActivityRequest req) {
        return ApiResponse.created(svc.log(req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/crm/activities/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }
}
