package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoAttendanceConfig {
    //Toạ độ văn phòng / địa điểm làm việc -
    private Double officeLatitude;   // Vĩ độ văn phòng (VD: 10.8231)
    private Double officeLongitude;  // Kinh độ văn phòng (VD: 106.6297)
    private Integer allowedRadiusMeters;  // Bán kính cho phép chấm công (mét), mặc định 300m

    // Danh sách WiFi cho phép
    // Cho phép nhiều SSID hoặc BSSID, phân cách bằng dấu phẩy
    private String allowedWifiSsids;   // Danh sách tên WiFi (SSID) được phép check-in
    private String allowedWifiBssids;  // Danh sách địa chỉ MAC (BSSID) được phép, VD: "AA:BB:CC:DD:EE:FF,11:22:33:44:55:66"
}
