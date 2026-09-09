package com.frezo.auth.mapper;

import com.frezo.auth.entity.UserSession;
import com.frezo.auth.model.CreateUserSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserSessionMapper {

    @Mapping(source = "ipAddress", target = "ipAddress")
    @Mapping(target = "loginTime", expression = "java(request.getNow() != null ? request.getNow() : java.time.LocalDateTime.now())")
    @Mapping(target = "lastActiveTime", expression = "java(request.getNow() != null ? request.getNow() : java.time.LocalDateTime.now())")
    @Mapping(target = "expiresAt", expression = "java((request.getNow() != null ? request.getNow() : java.time.LocalDateTime.now()).plusHours(24))")
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revokedBy", ignore = true)
    UserSession toEntity(CreateUserSession request);
}
