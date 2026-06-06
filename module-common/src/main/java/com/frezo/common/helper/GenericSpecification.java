package com.frezo.common.helper;

import org.springframework.data.jpa.domain.Specification;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class GenericSpecification {

    public static <T> Specification<T> likeField(String field, String value) {
        return (root, query, cb) -> {
            if (value != null && !value.isEmpty()) {
                try {
                    String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
                    String processedValue = SystemUtils.escapeAndRemoveAccentsSqlLike(decodedValue);
                    return cb.like(
                            cb.function("unaccent", String.class, cb.lower(root.get(field))),
                            "%" + processedValue.toLowerCase() + "%",
                            '\\');
                } catch (Exception e) {
                    String processedValue = SystemUtils.escapeAndRemoveAccentsSqlLike(value);
                    return cb.like(
                            cb.function("unaccent", String.class, cb.lower(root.get(field))),
                            "%" + processedValue.toLowerCase() + "%",
                            '\\');
                }
            }
            return cb.conjunction();
        };
    }

    public static <T> Specification<T> hasFieldIs(String field, Boolean value) {
        return ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(field), value));
    }

    public static <T> Specification<T> booleanField(String field, Boolean value) {
        return value == null ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    public static <T> Specification<T> equalField(String field, Object value) {
        return value == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    public static <T, V extends Comparable<? super V>>
    Specification<T> greaterThanOrEqual(String field, V value) {
        return value == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get(field), value);
    }

    public static <T, V extends Comparable<? super V>>
    Specification<T> lessThanOrEqual(String field, V value) {

        return value == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get(field), value);
    }

    /**
     * ThiNVQ : Filter LocalDate field theo tháng/năm cụ thể.
     * Dùng between(firstday, lastday) thay vì MONTH() function để tương thích mọi DB.
     */
    public static <T> Specification<T> monthOfDateField(String field, Integer month, Integer year) {
        return (month == null || year == null)
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> {
                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());
                    return cb.between(root.get(field), start, end);
                };
    }
}
