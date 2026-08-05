package com.frezo.qtht.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diễn giải cron 6 field (Spring) sang tiếng Việt cho màn hình quản trị tác vụ nền.
 * <p>
 * Chỉ cover các pattern thường dùng; biểu thức lạ trả về {@code "Theo lịch: {expression}"}
 * thay vì cố dịch sai.
 */
public final class CronDescriptionUtil {

    private CronDescriptionUtil() {
    }

    private static final Map<String, String> DOW_NAMES = new LinkedHashMap<>();

    static {
        DOW_NAMES.put("MON", "thứ Hai");
        DOW_NAMES.put("TUE", "thứ Ba");
        DOW_NAMES.put("WED", "thứ Tư");
        DOW_NAMES.put("THU", "thứ Năm");
        DOW_NAMES.put("FRI", "thứ Sáu");
        DOW_NAMES.put("SAT", "thứ Bảy");
        DOW_NAMES.put("SUN", "Chủ nhật");
        DOW_NAMES.put("0", "Chủ nhật");
        DOW_NAMES.put("1", "thứ Hai");
        DOW_NAMES.put("2", "thứ Ba");
        DOW_NAMES.put("3", "thứ Tư");
        DOW_NAMES.put("4", "thứ Năm");
        DOW_NAMES.put("5", "thứ Sáu");
        DOW_NAMES.put("6", "thứ Bảy");
        DOW_NAMES.put("7", "Chủ nhật");
    }

    public static String describe(String expression) {
        if (expression == null || expression.isBlank()) {
            return "Chưa cấu hình lịch";
        }
        String cron = expression.trim().replaceAll("\\s+", " ");
        String[] p = cron.split(" ");
        if (p.length != 6) {
            return fallback(expression);
        }

        String sec = p[0], min = p[1], hour = p[2], dom = p[3], month = p[4], dow = p[5];
        if (!"0".equals(sec) || !"*".equals(month)) {
            return fallback(expression);
        }

        String dowSuffix = describeDayOfWeek(dow);
        if (dowSuffix == null) {
            return fallback(expression);
        }

        // Mỗi phút / mỗi N phút
        if ("*".equals(hour) && "*".equals(dom)) {
            if ("*".equals(min)) {
                return join("Mỗi phút", dowSuffix);
            }
            if ("0".equals(min)) {
                return join("Đầu mỗi giờ", dowSuffix);
            }
            Integer everyMinutes = parseStep(min);
            if (everyMinutes != null) {
                return join("Mỗi " + everyMinutes + " phút", dowSuffix);
            }
            return fallback(expression);
        }

        // Giờ cố định
        Integer h = parseInt(hour);
        Integer m = parseInt(min);
        if (h == null || m == null) {
            return fallback(expression);
        }
        String time = String.format("%02d:%02d", h, m);

        if (!"*".equals(dom)) {
            Integer day = parseInt(dom);
            return day == null ? fallback(expression) : time + " ngày " + day + " hằng tháng";
        }
        if (dowSuffix.isEmpty()) {
            return time + " mỗi ngày";
        }
        return dowSuffix.contains("–")
                ? time + ", " + dowSuffix
                : time + " mỗi " + dowSuffix;
    }

    /**
     * @return "" khi mọi ngày trong tuần, mô tả tiếng Việt khi nhận diện được, null khi không hiểu.
     */
    private static String describeDayOfWeek(String dow) {
        if ("*".equals(dow) || "?".equals(dow)) {
            return "";
        }
        String upper = dow.toUpperCase();
        if (upper.contains("-")) {
            String[] range = upper.split("-");
            if (range.length != 2) return null;
            String from = DOW_NAMES.get(range[0]);
            String to = DOW_NAMES.get(range[1]);
            return (from == null || to == null) ? null : from + "–" + capitalize(to);
        }
        if (upper.contains(",")) {
            StringBuilder sb = new StringBuilder();
            for (String part : upper.split(",")) {
                String name = DOW_NAMES.get(part);
                if (name == null) return null;
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(name);
            }
            return sb.toString();
        }
        return DOW_NAMES.get(upper);
    }

    private static String join(String prefix, String dowSuffix) {
        return dowSuffix.isEmpty() ? prefix : prefix + ", " + dowSuffix;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static Integer parseStep(String field) {
        if (!field.startsWith("*/")) return null;
        return parseInt(field.substring(2));
    }

    private static Integer parseInt(String field) {
        try {
            return Integer.valueOf(field);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fallback(String expression) {
        return "Theo lịch: " + expression;
    }
}
