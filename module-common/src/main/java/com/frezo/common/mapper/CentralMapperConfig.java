package com.frezo.common.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Central config cho MỌI MapStruct mapper trong Frezo.
 * <p>
 * Xem chi tiết: {@code FrezoBE/SPRING_BOOT_BEST_PRACTICE.md §7 — MapStruct}.
 * <p>
 * Cách dùng:
 * <pre>
 * &#64;Mapper(config = CentralMapperConfig.class,
 *         uses = {OrganizationMapper.class, PersonMapper.class})
 * public interface DepartmentMapper {
 *     &#64;Mapping(target = "organizationName", source = "organization.name")
 *     DepartmentResponse toResponse(Department entity);
 * }
 * </pre>
 * <p>
 * Config chuẩn:
 * <ul>
 *   <li>{@code componentModel = "spring"} — Spring inject mapper</li>
 *   <li>{@code unmappedTargetPolicy = IGNORE} — không fail khi target có field không map (VD: id, audit trong BaseEntity)</li>
 *   <li>{@code unmappedSourcePolicy = IGNORE} — không warn khi source dư field</li>
 *   <li>{@code nullValueCheckStrategy = ALWAYS} — null-safe khi source null</li>
 *   <li>{@code nullValuePropertyMappingStrategy = IGNORE} — update partial: null source không ghi đè target</li>
 *   <li>{@code injectionStrategy = CONSTRUCTOR} — inject mapper qua constructor thay vì field</li>
 * </ul>
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface CentralMapperConfig {
}
