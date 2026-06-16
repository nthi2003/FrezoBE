package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.response.AttendanceResponse;
import com.frezo.qlns.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(target = "checkInLatitude", source = "latitude")
    @Mapping(target = "checkInLongitude", source = "longitude")
    @Mapping(target = "checkInWifiSsid", source = "wifiSsid")
    @Mapping(target = "checkInWifiBssid", source = "wifiBssid")
    Attendance toEntity(AttendanceCheckInRequest request);

    AttendanceResponse toResponse(Attendance entity);
}
