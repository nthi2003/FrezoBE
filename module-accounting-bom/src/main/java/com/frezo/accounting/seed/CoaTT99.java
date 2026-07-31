package com.frezo.accounting.seed;

import com.frezo.accounting.common.AccountType;

import java.util.ArrayList;
import java.util.List;

/**
 * Hệ thống tài khoản kế toán theo Thông tư 99/2025/TT-BTC (thay thế TT 200/2014),
 * hiệu lực từ 01/01/2026, áp dụng cho doanh nghiệp mọi lĩnh vực.
 * <p>Bản seed rút gọn — chi tiết hơn TT133 (tách 641/642, chi tiết BHXH...).
 */
public final class CoaTT99 {

    private CoaTT99() {}

    private static CoaSeedItem a(String code, String name, AccountType type, int level, String parent, boolean postable, boolean requiresPartner) {
        return new CoaSeedItem(code, name, type, level, parent, postable, requiresPartner);
    }

    public static final List<CoaSeedItem> ITEMS;

    static {
        List<CoaSeedItem> items = new ArrayList<>();

        // Loại 1 — Tài sản ngắn hạn
        items.add(a("111", "Tiền mặt", AccountType.ASSET, 1, null, false, false));
        items.add(a("1111", "Tiền Việt Nam", AccountType.ASSET, 2, "111", true, false));
        items.add(a("1112", "Ngoại tệ", AccountType.ASSET, 2, "111", true, false));
        items.add(a("1113", "Vàng tiền tệ", AccountType.ASSET, 2, "111", true, false));

        items.add(a("112", "Tiền gửi ngân hàng", AccountType.ASSET, 1, null, false, false));
        items.add(a("1121", "Tiền VN (NH)", AccountType.ASSET, 2, "112", true, false));
        items.add(a("1122", "Ngoại tệ (NH)", AccountType.ASSET, 2, "112", true, false));
        items.add(a("1123", "Vàng tiền tệ (NH)", AccountType.ASSET, 2, "112", true, false));

        items.add(a("113", "Tiền đang chuyển", AccountType.ASSET, 1, null, false, false));
        items.add(a("1131", "Tiền VN đang chuyển", AccountType.ASSET, 2, "113", true, false));
        items.add(a("1132", "Ngoại tệ đang chuyển", AccountType.ASSET, 2, "113", true, false));

        items.add(a("121", "Chứng khoán kinh doanh", AccountType.ASSET, 1, null, true, false));
        items.add(a("128", "Đầu tư nắm giữ đến ngày đáo hạn", AccountType.ASSET, 1, null, true, false));

        items.add(a("131", "Phải thu của khách hàng", AccountType.ASSET, 1, null, true, true));

        items.add(a("133", "Thuế GTGT được khấu trừ", AccountType.ASSET, 1, null, false, false));
        items.add(a("1331", "Thuế GTGT đầu vào hàng hóa DV", AccountType.ASSET, 2, "133", true, false));
        items.add(a("1332", "Thuế GTGT đầu vào TSCĐ", AccountType.ASSET, 2, "133", true, false));

        items.add(a("136", "Phải thu nội bộ", AccountType.ASSET, 1, null, false, false));
        items.add(a("1361", "Vốn kinh doanh ở đơn vị trực thuộc", AccountType.ASSET, 2, "136", true, false));
        items.add(a("1368", "Phải thu nội bộ khác", AccountType.ASSET, 2, "136", true, true));

        items.add(a("138", "Phải thu khác", AccountType.ASSET, 1, null, false, false));
        items.add(a("1381", "Tài sản thiếu chờ xử lý", AccountType.ASSET, 2, "138", true, false));
        items.add(a("1388", "Phải thu khác", AccountType.ASSET, 2, "138", true, true));

        items.add(a("141", "Tạm ứng", AccountType.ASSET, 1, null, true, true));

        items.add(a("151", "Hàng mua đang đi đường", AccountType.ASSET, 1, null, true, false));
        items.add(a("152", "Nguyên liệu, vật liệu", AccountType.ASSET, 1, null, true, false));
        items.add(a("153", "Công cụ, dụng cụ", AccountType.ASSET, 1, null, true, false));
        items.add(a("154", "Chi phí SXKD dở dang", AccountType.ASSET, 1, null, true, false));
        items.add(a("155", "Thành phẩm", AccountType.ASSET, 1, null, true, false));
        items.add(a("156", "Hàng hóa", AccountType.ASSET, 1, null, true, false));
        items.add(a("157", "Hàng gửi đi bán", AccountType.ASSET, 1, null, true, false));

        // Loại 2 — Tài sản dài hạn
        items.add(a("211", "Tài sản cố định", AccountType.ASSET, 1, null, true, false));
        items.add(a("213", "TSCĐ vô hình", AccountType.ASSET, 1, null, true, false));
        items.add(a("214", "Hao mòn TSCĐ", AccountType.ASSET, 1, null, false, false));
        items.add(a("2141", "Hao mòn TSCĐ hữu hình", AccountType.ASSET, 2, "214", true, false));
        items.add(a("2143", "Hao mòn TSCĐ vô hình", AccountType.ASSET, 2, "214", true, false));
        items.add(a("228", "Đầu tư khác", AccountType.ASSET, 1, null, true, true));
        items.add(a("242", "Chi phí trả trước", AccountType.ASSET, 1, null, true, false));
        items.add(a("244", "Cầm cố, thế chấp, ký quỹ, ký cược", AccountType.ASSET, 1, null, true, false));

        // Loại 3 — Nợ phải trả
        items.add(a("331", "Phải trả cho người bán", AccountType.LIABILITY, 1, null, true, true));

        items.add(a("333", "Thuế và các khoản phải nộp NN", AccountType.LIABILITY, 1, null, false, false));
        items.add(a("3331", "Thuế GTGT phải nộp", AccountType.LIABILITY, 2, "333", true, false));
        items.add(a("33311", "Thuế GTGT đầu ra", AccountType.LIABILITY, 3, "3331", true, false));
        items.add(a("33312", "Thuế GTGT hàng nhập khẩu", AccountType.LIABILITY, 3, "3331", true, false));
        items.add(a("3332", "Thuế tiêu thụ đặc biệt", AccountType.LIABILITY, 2, "333", true, false));
        items.add(a("3333", "Thuế xuất, nhập khẩu", AccountType.LIABILITY, 2, "333", true, false));
        items.add(a("3334", "Thuế TNDN", AccountType.LIABILITY, 2, "333", true, false));
        items.add(a("3335", "Thuế TNCN", AccountType.LIABILITY, 2, "333", true, false));
        items.add(a("3338", "Thuế và phí khác", AccountType.LIABILITY, 2, "333", true, false));

        items.add(a("334", "Phải trả người lao động", AccountType.LIABILITY, 1, null, true, true));

        items.add(a("335", "Chi phí phải trả", AccountType.LIABILITY, 1, null, true, false));

        items.add(a("338", "Phải trả, phải nộp khác", AccountType.LIABILITY, 1, null, false, false));
        items.add(a("3382", "Kinh phí công đoàn", AccountType.LIABILITY, 2, "338", true, false));
        items.add(a("3383", "Bảo hiểm xã hội", AccountType.LIABILITY, 2, "338", true, false));
        items.add(a("3384", "Bảo hiểm y tế", AccountType.LIABILITY, 2, "338", true, false));
        items.add(a("3386", "Bảo hiểm thất nghiệp", AccountType.LIABILITY, 2, "338", true, false));
        items.add(a("3388", "Phải trả, phải nộp khác", AccountType.LIABILITY, 2, "338", true, true));

        items.add(a("341", "Vay và nợ thuê tài chính", AccountType.LIABILITY, 1, null, true, true));
        items.add(a("344", "Nhận ký quỹ, ký cược", AccountType.LIABILITY, 1, null, true, true));

        // Loại 4 — Vốn CSH
        items.add(a("411", "Vốn đầu tư của chủ sở hữu", AccountType.EQUITY, 1, null, true, false));
        items.add(a("414", "Quỹ đầu tư phát triển", AccountType.EQUITY, 1, null, true, false));
        items.add(a("421", "LNST chưa phân phối", AccountType.EQUITY, 1, null, false, false));
        items.add(a("4211", "LNST năm trước", AccountType.EQUITY, 2, "421", true, false));
        items.add(a("4212", "LNST năm nay", AccountType.EQUITY, 2, "421", true, false));

        // Loại 5 — Doanh thu (511 = cha; ghi sổ vào leaf 511x — khớp Invoice postToGL)
        items.add(a("511", "Doanh thu bán hàng & cung cấp DV", AccountType.REVENUE, 1, null, false, false));
        items.add(a("5111", "Doanh thu bán hàng hóa", AccountType.REVENUE, 2, "511", true, false));
        items.add(a("5112", "Doanh thu bán thành phẩm", AccountType.REVENUE, 2, "511", true, false));
        items.add(a("5113", "Doanh thu cung cấp dịch vụ", AccountType.REVENUE, 2, "511", true, false));
        items.add(a("515", "Doanh thu hoạt động tài chính", AccountType.REVENUE, 1, null, true, false));
        items.add(a("521", "Các khoản giảm trừ doanh thu", AccountType.REVENUE, 1, null, true, false));

        // Loại 6 — Chi phí SXKD
        items.add(a("611", "Mua hàng (kiểm kê định kỳ)", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("621", "Chi phí NVL trực tiếp", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("622", "Chi phí nhân công trực tiếp", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("627", "Chi phí sản xuất chung", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("632", "Giá vốn hàng bán", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("635", "Chi phí tài chính", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("641", "Chi phí bán hàng", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("642", "Chi phí quản lý doanh nghiệp", AccountType.EXPENSE, 1, null, true, false));

        // Loại 7/8
        items.add(a("711", "Thu nhập khác", AccountType.REVENUE, 1, null, true, false));
        items.add(a("811", "Chi phí khác", AccountType.EXPENSE, 1, null, true, false));
        items.add(a("821", "Chi phí thuế TNDN", AccountType.EXPENSE, 1, null, true, false));

        // Loại 9
        items.add(a("911", "Xác định kết quả kinh doanh", AccountType.CLEARING, 1, null, true, false));

        ITEMS = List.copyOf(items);
    }
}
