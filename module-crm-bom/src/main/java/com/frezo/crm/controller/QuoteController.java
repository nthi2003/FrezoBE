package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.dto.QuoteRequest;
import com.frezo.crm.service.QuoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crm/quotes")
@RequiredArgsConstructor
@Tag(name = "CRM - Quotes")
public class QuoteController {

    private final QuoteService svc;

    @GetMapping
    @CheckPermission(api = "/crm/quotes", action = "VIEW")
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    @CheckPermission(api = "/crm/quotes/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(java.util.Map.of(
                "quote", svc.get(id),
                "items", svc.items(id)));
    }

    @PostMapping
    @CheckPermission(api = "/crm/quotes", action = "CREATE")
    public ApiResponse<?> create(@RequestBody @Valid QuoteRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/crm/quotes/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid QuoteRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/crm/quotes/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/{id}/send")
    @CheckPermission(api = "/crm/quotes/{id}/send", action = "UPDATE")
    public ApiResponse<?> send(@PathVariable String id) { return ApiResponse.ok(svc.send(id)); }

    @PatchMapping("/{id}/accept")
    @CheckPermission(api = "/crm/quotes/{id}/accept", action = "UPDATE")
    public ApiResponse<?> accept(@PathVariable String id) { return ApiResponse.ok(svc.accept(id)); }

    @PatchMapping("/{id}/reject")
    @CheckPermission(api = "/crm/quotes/{id}/reject", action = "UPDATE")
    public ApiResponse<?> reject(@PathVariable String id) { return ApiResponse.ok(svc.reject(id)); }
}
