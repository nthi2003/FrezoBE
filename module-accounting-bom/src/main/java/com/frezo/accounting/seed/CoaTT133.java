package com.frezo.accounting.seed;

import com.frezo.accounting.common.AccountType;

import java.util.List;

/**
 * Hệ thống tài khoản kế toán rút gọn theo Thông tư 133/2016/TT-BTC
 * (Doanh nghiệp nhỏ và vừa). Chỉ seed các TK phổ biến nhất, user có thể thêm.
 */
public final class CoaTT133 {

    private CoaTT133() {}

    // Helper alias
    private static CoaSeedItem a(String code, String name, AccountType type, int level, String parent, boolean postable, boolean requiresPartner) {
        return new CoaSeedItem(code, name, type, level, parent, postable, requiresPartner);
    }

    public static final List<CoaSeedItem> ITEMS = List.of(
            // ==== Loại 1: Tài sản ngắn hạn ====
            a("111", "Tiền mặt", AccountType.ASSET, 1, null, false, false),
            a("1111", "Tiền Việt Nam", AccountType.ASSET, 2, "111", true, false),
            a("1112", "Ngoại tệ", AccountType.ASSET, 2, "111", true, false),

            a("112", "Tiền gửi ngân hàng", AccountType.ASSET, 1, null, false, false),
            a("1121", "Tiền Việt Nam (NH)", AccountType.ASSET, 2, "112", true, false),
            a("1122", "Ngoại tệ (NH)", AccountType.ASSET, 2, "112", true, false),

            a("121", "Chứng khoán kinh doanh", AccountType.ASSET, 1, null, true, false),
            a("128", "Đầu tư nắm giữ đến ngày đáo hạn", AccountType.ASSET, 1, null, true, false),

            a("131", "Phải thu của khách hàng", AccountType.ASSET, 1, null, true, true),

            a("133", "Thuế GTGT được khấu trừ", AccountType.ASSET, 1, null, false, false),
            a("1331", "Thuế GTGT được khấu trừ hàng hóa dịch vụ", AccountType.ASSET, 2, "133", true, false),
            a("1332", "Thuế GTGT được khấu trừ của TSCĐ", AccountType.ASSET, 2, "133", true, false),

            a("138", "Phải thu khác", AccountType.ASSET, 1, null, false, false),
            a("1381", "Tài sản thiếu chờ xử lý", AccountType.ASSET, 2, "138", true, false),
            a("1386", "Cầm cố, thế chấp, ký quỹ, ký cược", AccountType.ASSET, 2, "138", true, false),
            a("1388", "Phải thu khác", AccountType.ASSET, 2, "138", true, true),

            a("141", "Tạm ứng", AccountType.ASSET, 1, null, true, true),

            a("151", "Hàng mua đang đi đường", AccountType.ASSET, 1, null, true, false),
            a("152", "Nguyên liệu, vật liệu", AccountType.ASSET, 1, null, true, false),
            a("153", "Công cụ, dụng cụ", AccountType.ASSET, 1, null, true, false),
            a("154", "Chi phí SXKD dở dang", AccountType.ASSET, 1, null, true, false),
            a("155", "Thành phẩm", AccountType.ASSET, 1, null, true, false),
            a("156", "Hàng hóa", AccountType.ASSET, 1, null, true, false),
            a("157", "Hàng gửi đi bán", AccountType.ASSET, 1, null, true, false),

            // ==== Loại 2: Tài sản dài hạn ====
            a("211", "Tài sản cố định", AccountType.ASSET, 1, null, false, false),
            a("2111", "TSCĐ hữu hình", AccountType.ASSET, 2, "211", true, false),
            a("2112", "TSCĐ thuê tài chính", AccountType.ASSET, 2, "211", true, false),
            a("2113", "TSCĐ vô hình", AccountType.ASSET, 2, "211", true, false),

            a("214", "Hao mòn TSCĐ", AccountType.ASSET, 1, null, false, false),
            a("2141", "Hao mòn TSCĐ hữu hình", AccountType.ASSET, 2, "214", true, false),
            a("2142", "Hao mòn TSCĐ thuê TC", AccountType.ASSET, 2, "214", true, false),
            a("2143", "Hao mòn TSCĐ vô hình", AccountType.ASSET, 2, "214", true, false),

            a("228", "Đầu tư góp vốn vào đơn vị khác", AccountType.ASSET, 1, null, true, true),
            a("242", "Chi phí trả trước", AccountType.ASSET, 1, null, true, false),

            // ==== Loại 3: Nợ phải trả ====
            a("331", "Phải trả cho người bán", AccountType.LIABILITY, 1, null, true, true),

            a("333", "Thuế và các khoản phải nộp NN", AccountType.LIABILITY, 1, null, false, false),
            a("3331", "Thuế GTGT phải nộp", AccountType.LIABILITY, 2, "333", true, false),
            a("33311", "Thuế GTGT đầu ra", AccountType.LIABILITY, 3, "3331", true, false),
            a("33312", "Thuế GTGT hàng nhập khẩu", AccountType.LIABILITY, 3, "3331", true, false),
            a("3334", "Thuế TNDN", AccountType.LIABILITY, 2, "333", true, false),
            a("3335", "Thuế TNCN", AccountType.LIABILITY, 2, "333", true, false),
            a("3338", "Thuế khác", AccountType.LIABILITY, 2, "333", true, false),

            a("334", "Phải trả người lao động", AccountType.LIABILITY, 1, null, true, true),

            a("335", "Chi phí phải trả", AccountType.LIABILITY, 1, null, true, false),

            a("338", "Phải trả, phải nộp khác", AccountType.LIABILITY, 1, null, false, false),
            a("3382", "Kinh phí công đoàn", AccountType.LIABILITY, 2, "338", true, false),
            a("3383", "Bảo hiểm xã hội", AccountType.LIABILITY, 2, "338", true, false),
            a("3384", "Bảo hiểm y tế", AccountType.LIABILITY, 2, "338", true, false),
            a("3385", "Bảo hiểm thất nghiệp", AccountType.LIABILITY, 2, "338", true, false),
            a("3386", "Nhận ký quỹ, ký cược", AccountType.LIABILITY, 2, "338", true, true),
            a("3388", "Phải trả, phải nộp khác", AccountType.LIABILITY, 2, "338", true, true),

            a("341", "Vay và nợ thuê tài chính", AccountType.LIABILITY, 1, null, true, true),

            // ==== Loại 4: Vốn chủ sở hữu ====
            a("411", "Vốn đầu tư của chủ sở hữu", AccountType.EQUITY, 1, null, true, false),
            a("418", "Các quỹ thuộc VCSH", AccountType.EQUITY, 1, null, true, false),
            a("421", "Lợi nhuận sau thuế chưa phân phối", AccountType.EQUITY, 1, null, false, false),
            a("4211", "LNST năm trước", AccountType.EQUITY, 2, "421", true, false),
            a("4212", "LNST năm nay", AccountType.EQUITY, 2, "421", true, false),

            // ==== Loại 5: Doanh thu ====
            a("511", "Doanh thu bán hàng và cung cấp DV", AccountType.REVENUE, 1, null, false, false),
            a("5111", "Doanh thu bán hàng hóa", AccountType.REVENUE, 2, "511", true, false),
            a("5112", "Doanh thu bán thành phẩm", AccountType.REVENUE, 2, "511", true, false),
            a("5113", "Doanh thu cung cấp dịch vụ", AccountType.REVENUE, 2, "511", true, false),
            a("515", "Doanh thu hoạt động tài chính", AccountType.REVENUE, 1, null, true, false),

            // ==== Loại 6: Chi phí SXKD ====
            a("611", "Mua hàng (dùng cho phương pháp kiểm kê định kỳ)", AccountType.EXPENSE, 1, null, true, false),
            a("631", "Giá thành SX", AccountType.EXPENSE, 1, null, true, false),
            a("632", "Giá vốn hàng bán", AccountType.EXPENSE, 1, null, true, false),
            a("635", "Chi phí tài chính", AccountType.EXPENSE, 1, null, true, false),

            a("642", "Chi phí quản lý kinh doanh", AccountType.EXPENSE, 1, null, false, false),
            a("6421", "Chi phí bán hàng", AccountType.EXPENSE, 2, "642", true, false),
            a("6422", "Chi phí quản lý doanh nghiệp", AccountType.EXPENSE, 2, "642", true, false),

            // ==== Loại 7 / 8 ====
            a("711", "Thu nhập khác", AccountType.REVENUE, 1, null, true, false),
            a("811", "Chi phí khác", AccountType.EXPENSE, 1, null, true, false),
            a("821", "Chi phí thuế TNDN", AccountType.EXPENSE, 1, null, true, false),

            // ==== Loại 9: Xác định KQKD ====
            a("911", "Xác định kết quả kinh doanh", AccountType.CLEARING, 1, null, true, false)
    );
}
