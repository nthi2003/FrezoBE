package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.AffiliateLinkRequest;
import com.frezo.fbautomation.dto.response.AffiliateLinkResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AffiliateLinkService — quản lý affiliate link + tracker click/conversion.
 * <p>
 * Không cần API bên ngoài — hoạt động độc lập, dùng cho program KOL/CTV nội bộ.
 */
public interface AffiliateLinkService {

    AffiliateLinkResponse create(AffiliateLinkRequest req);

    AffiliateLinkResponse update(String id, AffiliateLinkRequest req);

    AffiliateLinkResponse get(String id);

    void delete(String id);

    List<AffiliateLinkResponse> list(String campaign, String status, String kolName);

    /**
     * Resolve slug → targetUrl (với UTM đã append).
     * Ghi {@link com.frezo.fbautomation.entity.AffiliateClick} và tăng counter.
     *
     * @return URL đích để controller redirect 302
     */
    String trackAndResolve(String code, String ip, String userAgent, String referer);

    /**
     * Đánh dấu conversion khi có order/lead được tạo từ affiliate link.
     * BE khác (product/customer) gọi hàm này sau khi tạo entity thành công.
     *
     * @param code   slug link
     * @param value  giá trị đơn hàng (dùng tính commission)
     * @return true nếu match được click gần nhất (attribution 30 ngày)
     */
    boolean recordConversion(String code, BigDecimal value);

    /** Dashboard tổng hợp — top campaign, top KOL, tổng click/conversion/revenue. */
    Map<String, Object> dashboard();
}
