package com.frezo.cms.service;

import com.frezo.cms.entity.Restaurant;
import com.frezo.cms.repository.RestaurantRepository;
import com.frezo.common.audit.AuditLogService;
import com.frezo.common.security.CryptoUtils;
import com.frezo.cms.dto.response.CustomerExportResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerControlService {

    private final RestaurantRepository restaurantRepository;
    private final CryptoUtils cryptoUtils;
    private final AuditLogService auditLogService;

    // Xuất CSV thông tin khách hàng kèm Audit Log (Thỏa mãn tính năng 1 & 4.1)
    public String exportCustomersToCsv(HttpServletRequest request) {
        List<Restaurant> customers = restaurantRepository.findAll();

        StringWriter writer = new StringWriter();
        try (PrintWriter csvWriter = new PrintWriter(writer)) {
            // Header
            csvWriter.println("Mã KH,Tên Khách Hàng,SĐT,Địa Chỉ,Hạn Mức Nợ");

            for (Restaurant r : customers) {
                // Giải mã số điện thoại
                String plainPhone = r.getPhoneEncrypted() != null ?
                    cryptoUtils.decrypt(r.getPhoneEncrypted()) : "N/A";

                csvWriter.printf("%s,%s,%s,%s,%f\n",
                        r.getCode(),
                        r.getName(),
                        plainPhone,
                        r.getAddress() != null ? r.getAddress().replace(",", " ") : "",
                        r.getCreditLimit());
            }

            // Ghi Audit Log khi có thao tác xuất SĐT (Bảo mật)
            auditLogService.logAction(
                "EXPORT",
                "Restaurant",
                "ALL",
                "Người dùng đã xuất toàn bộ file danh sách Khách hàng bao gồm số điện thoại gốc.",
                request
            );

            return writer.toString();
        }
    }
}
