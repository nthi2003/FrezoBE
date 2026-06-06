package com.frezo.cms.controller;

import com.frezo.cms.service.CustomerControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/cms/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management API", description = "CMS Customer operations")
public class CustomerControlController {

    private final CustomerControlService customerControlService;

    @GetMapping("/export")
    @Operation(summary = "Export customers to CSV with decrypted phone numbers")
    public void exportCustomers(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String csvContent = customerControlService.exportCustomersToCsv(request);
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=customers.csv");
        response.getWriter().write(csvContent);
    }
}
