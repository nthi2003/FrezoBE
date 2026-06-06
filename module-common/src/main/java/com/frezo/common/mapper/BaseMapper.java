package com.frezo.common.mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base Mapper Interface for MapStruct
 *
 * @param <D> DTO type
 * @param <E> Entity type
 */
public interface BaseMapper<D, E> {

    E toEntity(D dto);

    D toDto(E entity);

    default List<E> toEntity(List<D> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    default List<D> toDto(List<E> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }
}
