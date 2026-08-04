package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/crm/invoices", action = "VIEW")
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    @CheckPermission(api = "/crm/invoices/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(java.util.Map.of(
                "invoice", svc.get(id),
                "items", svc.items(id)));
    }

    @PostMapping
    @CheckPermission(api = "/crm/invoices", action = "CREATE")
    public ApiResponse<?> create(@RequestBody @Valid InvoiceRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/crm/invoices/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid InvoiceRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/crm/invoices/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/{id}/issue")
    @CheckPermission(api = "/crm/invoices/{id}/issue", action = "UPDATE")
    public ApiResponse<?> issue(@PathVariable String id) { return ApiResponse.ok(svc.issue(id)); }

    @PostMapping("/{id}/post-to-gl")
    @CheckPermission(api = "/crm/invoices/{id}/post-to-gl", action = "CREATE")
    public ApiResponse<?> postToGL(@PathVariable String id) {
        return ApiResponse.ok(svc.postToGL(id));
    }

    @PostMapping("/{id}/record-payment")
    @CheckPermission(api = "/crm/invoices/{id}/record-payment", action = "CREATE")
    public ApiResponse<?> recordPayment(@PathVariable String id,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam(required = false) String paymentAccountCode) {
        return ApiResponse.ok(svc.recordPayment(id, amount, paymentAccountCode));
    }
}
