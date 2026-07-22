package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.crm.dto.InvoiceRequest;
import com.frezo.crm.service.InvoiceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/crm/invoices")
@RequiredArgsConstructor
@Tag(name = "CRM - Invoices")
public class InvoiceController {

    private final InvoiceService svc;

    @GetMapping
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(java.util.Map.of(
                "invoice", svc.get(id),
                "items", svc.items(id)));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid InvoiceRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid InvoiceRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/{id}/issue")
    public ApiResponse<?> issue(@PathVariable String id) { return ApiResponse.ok(svc.issue(id)); }

    @PostMapping("/{id}/post-to-gl")
    public ApiResponse<?> postToGL(@PathVariable String id) {
        return ApiResponse.ok(svc.postToGL(id));
    }

    @PostMapping("/{id}/record-payment")
    public ApiResponse<?> recordPayment(@PathVariable String id,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam(required = false) String paymentAccountCode) {
        return ApiResponse.ok(svc.recordPayment(id, amount, paymentAccountCode));
    }
}
