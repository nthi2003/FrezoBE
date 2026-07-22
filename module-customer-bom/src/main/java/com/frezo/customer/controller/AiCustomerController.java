package com.frezo.customer.controller;

import com.frezo.customer.service.CustomerService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/ai")
@RequiredArgsConstructor
@Tag(name = "Tích hợp AI", description = "Đồng bộ dữ liệu khách hàng từ hệ thống AI Scraper")
public class AiCustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Quét và đồng bộ khách hàng từ Google Maps", 
               description = "Gọi hệ thống AI để tìm kiếm quán/văn phòng theo từ khóa và lưu vào danh mục khách hàng tiềm năng.")
    @PostMapping("/sync")
    public Response<String> syncFromAi(
            @RequestParam String keyword,
            @RequestParam String city,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        int count = customerService.syncLeadsFromAi(keyword, city, ward, limit);
        return Response.ok("Đã đồng bộ thành công " + count + " khách hàng mới từ AI.");
    }
}
