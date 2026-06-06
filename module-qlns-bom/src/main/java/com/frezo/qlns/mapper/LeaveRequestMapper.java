package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestResponse;
import com.frezo.qlns.entity.LeaveRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {
    LeaveRequest toEntity(LeaveRequestAddRequest request);
    LeaveRequestResponse toResponse(LeaveRequest entity);
}
