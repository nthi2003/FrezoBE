package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qlns.common.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Chấm công nhân viên (nâng cao)
 * - contractId / personId: liên kết nhân viên
 * - attendanceDate: ngày chấm công
 * - checkInTime / checkOutTime: giờ vào/ra
 * - workMinutes: số phút làm việc thực tế
 * - lateMinutes: số phút đi muộn
 * - overtimeMinutes: số phút làm thêm giờ
 * - shiftType: loại ca (MORNING/AFTERNOON/FULL/NIGHT)
 * - status: PRESENT / ABSENT / LATE / HALF_DAY / LEAVE / HOLIDAY
 * - approvedBy: người duyệt (nếu có điều chỉnh)
 * - note: ghi chú
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "attendance_date"}))
public class Attendance extends BaseEntity {

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "check_in_wifi_ssid", length = 100)
    private String checkInWifiSsid;

    @Column(name = "check_in_wifi_bssid", length = 50)
    private String checkInWifiBssid;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "check_out_latitude")
    private Double checkOutLatitude;

    @Column(name = "check_out_longitude")
    private Double checkOutLongitude;

    @Column(name = "check_out_wifi_ssid", length = 100)
    private String checkOutWifiSsid;

    @Column(name = "check_out_wifi_bssid", length = 50)
    private String checkOutWifiBssid;

    /** Số phút làm việc thực tế */
    @Column(name = "work_minutes")
    private Integer workMinutes;

    /** Số phút đi muộn (0 nếu đúng giờ) */
    @Column(name = "late_minutes")
    private Integer lateMinutes;

    /** Số phút làm thêm giờ */
    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    /** Loại ca: MORNING / AFTERNOON / FULL / NIGHT */
    @Column(name = "shift_type", length = 20)
    private String shiftType;

    /** PRESENT / ABSENT / LATE / HALF_DAY / LEAVE / HOLIDAY */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AttendanceStatus status;

    /** Người duyệt khi điều chỉnh chấm công thủ công */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "note", length = 500)
    private String note;
}
