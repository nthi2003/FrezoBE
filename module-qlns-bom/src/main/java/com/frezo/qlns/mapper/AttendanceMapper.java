package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.response.AttendanceResponse;
import com.frezo.qlns.entity.Attendance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {
    Attendance toEntity(AttendanceCheckInRequest request);
    AttendanceResponse toResponse(Attendance entity);
}
