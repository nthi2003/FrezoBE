package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
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
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(java.util.Map.of(
                "quote", svc.get(id),
                "items", svc.items(id)));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid QuoteRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid QuoteRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/{id}/send")
    public ApiResponse<?> send(@PathVariable String id) { return ApiResponse.ok(svc.send(id)); }

    @PatchMapping("/{id}/accept")
    public ApiResponse<?> accept(@PathVariable String id) { return ApiResponse.ok(svc.accept(id)); }

    @PatchMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable String id) { return ApiResponse.ok(svc.reject(id)); }
}
