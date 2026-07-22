package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestResponse;
import com.frezo.qlns.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeaveRequestMapper {

    /**
     * Bỏ qua các field workflow ở entity — service sẽ set sau khi resolve
     * (VD: managerUsername, status, ...).
     */
    LeaveRequest toEntity(LeaveRequestAddRequest request);

    /**
     * Entity → Response: MapStruct auto-map same-name fields.
     * {@code createdDate} là {@code LocalDateTime} ở entity nhưng {@code String} ở response
     * → dùng expression để convert (ISO-8601). FE parse bằng {@code new Date(iso)}.
     */
    @Mapping(target = "createdDate",
            expression = "java(entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null)")
    LeaveRequestResponse toResponse(LeaveRequest entity);
}
