package com.frezo.product.service.impl.product;

import com.frezo.product.dto.request.PriceUpdateRequest;
import com.frezo.product.dto.response.ProductImportResultResponse;
import com.frezo.product.entity.PriceGroup;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.PriceGroupRepository;
import com.frezo.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductExcelImportService {

    private static final String CATEGORY_GROUP = "LoaiSanPham";
    private static final String DEFAULT_UNIT = "kg";

    private static final String COL_CODE = "code";
    private static final String COL_NAME = "name";
    private static final String COL_CATEGORY = "category";
    private static final String COL_RETAIL = "retail";
    private static final String COL_RETAIL_SHIP = "retailShip";
    private static final String COL_WHOLESALE = "wholesale";
    private static final String COL_SUPER_WHOLESALE = "superWholesale";
    private static final String COL_STATUS = "status";

    private final ProductRepository productRepository;
    private final PriceGroupRepository priceGroupRepository;
    private final ProductPricingService pricingService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /** Nhập sản phẩm từ file Excel, đọc từng dòng và upsert vào database. */
    public ProductImportResultResponse importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File Excel trống");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file .xlsx hoặc .xls");
        }

        int successCount = 0;
        int failCount = 0;
        List<ProductImportResultResponse.RowError> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("Sheet Excel trống");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> colMap = mapHeaders(headerRow);
            if (!colMap.containsKey(COL_CODE) || !colMap.containsKey(COL_NAME)) {
                throw new IllegalArgumentException(
                        "Thiếu cột bắt buộc: Mã mặt hàng / Tên mặt hàng");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            int firstData = sheet.getFirstRowNum() + 1;
            int last = sheet.getLastRowNum();

            for (int r = firstData; r <= last; r++) {
                Row row = sheet.getRow(r);
                int excelRow = r + 1; // 1-based for user
                if (row == null || isBlankRow(row, colMap, formatter)) {
                    continue;
                }
                try {
                    transactionTemplate.executeWithoutResult(status -> upsertRow(row, colMap, formatter));
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    errors.add(ProductImportResultResponse.RowError.builder()
                            .row(excelRow)
                            .message(msg)
                            .build());
                    log.warn("Product Excel import row {} failed: {}", excelRow, msg);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đọc được file Excel: " + e.getMessage(), e);
        }

        return ProductImportResultResponse.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    /** Đọc 1 dòng Excel, tạo/cập nhật sản phẩm và cập nhật giá theo nhóm giá. */
    private void upsertRow(Row row, Map<String, Integer> colMap, DataFormatter formatter) {
        String code = normalizeCode(cellText(row, colMap.get(COL_CODE), formatter));
        String name = trimToNull(cellText(row, colMap.get(COL_NAME), formatter));
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã mặt hàng");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Thiếu tên mặt hàng");
        }
        if (code.length() > 30) {
            throw new IllegalArgumentException("Mã quá dài (tối đa 30 ký tự): " + code);
        }

        String categoryRaw = trimToNull(cellText(row, colMap.get(COL_CATEGORY), formatter));
        String categoryId = resolveCategoryId(categoryRaw);
        if (categoryId == null && categoryRaw != null) {
            throw new IllegalArgumentException("Không tìm thấy danh mục: " + categoryRaw);
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Thiếu loại / danh mục");
        }

        Double retailShip = parseVnd(cellText(row, colMap.get(COL_RETAIL_SHIP), formatter));
        Double retail = parseVnd(cellText(row, colMap.get(COL_RETAIL), formatter));
        Double listPrice = retailShip != null ? retailShip : retail;
        if (listPrice == null) {
            throw new IllegalArgumentException("Thiếu giá bán lẻ");
        }

        Double wholesale = parseVnd(cellText(row, colMap.get(COL_WHOLESALE), formatter));
        Double superWholesale = parseVnd(cellText(row, colMap.get(COL_SUPER_WHOLESALE), formatter));
        Boolean isActive = parseStatus(cellText(row, colMap.get(COL_STATUS), formatter));

        Product product = productRepository.findByCode(code).orElse(null);
        if (product == null) {
            product = Product.builder()
                    .code(code)
                    .name(name)
                    .categoryId(categoryId)
                    .price(listPrice)
                    .isActive(isActive != null ? isActive : true)
                    .isNew(true)
                    .viewCount(0L)
                    .build();
        } else {
            product.setName(name);
            product.setCategoryId(categoryId);
            product.setPrice(listPrice);
            if (isActive != null) {
                product.setIsActive(isActive);
            }
            product.setIsDeleted(false);
        }
        productRepository.save(product);

        ensurePriceGroups();
        List<PriceUpdateRequest.UnitPrice> unitPrices = new ArrayList<>();
        Double retailForTier = retail != null ? retail : listPrice;
        if (retailForTier != null) {
            unitPrices.add(unitPrice(DEFAULT_UNIT, "RETAIL", retailForTier));
        }
        if (wholesale != null) {
            unitPrices.add(unitPrice(DEFAULT_UNIT, "WHOLESALE_10KG", wholesale));
        }
        if (superWholesale != null) {
            unitPrices.add(unitPrice(DEFAULT_UNIT, "SUPER_WHOLESALE", superWholesale));
        }
        if (!unitPrices.isEmpty()) {
            PriceUpdateRequest req = new PriceUpdateRequest();
            req.setProductCode(code);
            req.setUnitPrices(unitPrices);
            pricingService.bulkUpdatePrices(List.of(req));
        }
    }

    /** Đảm bảo các nhóm giá mặc định (bán lẻ, bán sỉ, siêu sỉ) tồn tại. */
    private void ensurePriceGroups() {
        ensureGroup("RETAIL", "Giá bán lẻ");
        ensureGroup("WHOLESALE_10KG", "Giá bán sỉ (10kg)");
        ensureGroup("SUPER_WHOLESALE", "Giá siêu sỉ (30kg+)");
    }

    /** Tạo mới nhóm giá nếu chưa tồn tại theo mã. */
    private void ensureGroup(String code, String name) {
        if (priceGroupRepository.findByCode(code).isEmpty()) {
            priceGroupRepository.save(PriceGroup.builder()
                    .code(code)
                    .name(name)
                    .isActive(true)
                    .build());
        }
    }

    /** Tạo đối tượng UnitPrice từ đơn vị, nhóm giá và giá mới. */
    private static PriceUpdateRequest.UnitPrice unitPrice(String unit, String group, Double price) {
        PriceUpdateRequest.UnitPrice up = new PriceUpdateRequest.UnitPrice();
        up.setUnitName(unit);
        up.setPriceGroupCode(group);
        up.setNewPrice(price);
        return up;
    }

    /** Tìm danh mục theo tên/mã/shortName với fuzzy match, ưu tiên kết quả chính xác nhất. */
    @SuppressWarnings("unchecked")
    private String resolveCategoryId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String q = raw.trim();
        List<String> ids = entityManager.createNativeQuery("""
                        SELECT id FROM categories
                        WHERE COALESCE(is_deleted, false) = false
                          AND group_code = :group
                          AND (
                               lower(name) = lower(:q)
                            OR lower(code) = lower(:q)
                            OR lower(COALESCE(short_name, '')) = lower(:q)
                            OR lower(name) LIKE lower(:q) || '%'
                            OR lower(name) LIKE '%' || lower(:q) || '%'
                          )
                        ORDER BY
                          CASE
                            WHEN lower(name) = lower(:q) THEN 0
                            WHEN lower(code) = lower(:q) THEN 1
                            WHEN lower(COALESCE(short_name, '')) = lower(:q) THEN 2
                            WHEN lower(name) LIKE lower(:q) || '%' THEN 3
                            ELSE 4
                          END
                        LIMIT 1
                        """)
                .setParameter("group", CATEGORY_GROUP)
                .setParameter("q", q)
                .getResultList();
        return ids.isEmpty() ? null : String.valueOf(ids.get(0));
    }

    /** Đọc dòng header Excel và ánh xạ tên cột sang key chuẩn. */
    private Map<String, Integer> mapHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) {
            return map;
        }
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (Cell cell : headerRow) {
            String raw = formatter.formatCellValue(cell);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String norm = normalizeHeader(raw);
            if (isPercentHeader(norm)) {
                continue; // CẤM import cột %
            }
            String key = classifyHeader(norm);
            if (key != null && !map.containsKey(key)) {
                map.put(key, cell.getColumnIndex());
            }
        }
        return map;
    }

    /** Kiểm tra header có chứa ký hiệu phần trăm (%) — không import cột % giá. */
    private static boolean isPercentHeader(String norm) {
        if (norm.startsWith("%") || norm.contains("% ")) {
            return true;
        }
        return norm.contains("%le") || norm.contains("%ship") || norm.contains("%si")
                || norm.contains("%sieusi") || norm.equals("%");
    }

    /** Phân loại tên cột Excel sang key chuẩn hóa (code, name, category, ...). */
    private static String classifyHeader(String norm) {
        // strip accents for matching
        String n = stripAccents(norm).replace(" ", "");

        if (n.contains("mamathang") || n.equals("ma") || n.equals("code") || n.equals("masp")) {
            return COL_CODE;
        }
        if (n.contains("tenmathang") || n.equals("ten") || n.equals("name") || n.equals("tensp")) {
            return COL_NAME;
        }
        if (n.equals("loai") || n.contains("danhmuc") || n.equals("category")) {
            return COL_CATEGORY;
        }
        if (n.contains("giabanle") && (n.contains("gomship") || n.contains("dagom") || n.contains("dagomship"))) {
            return COL_RETAIL_SHIP;
        }
        if (n.contains("giabanle") && !n.contains("si") && !n.contains("%")) {
            return COL_RETAIL;
        }
        if (n.contains("sieusi") || n.contains("giabanlesieusi") || (n.contains("sieu") && n.contains("si"))) {
            return COL_SUPER_WHOLESALE;
        }
        if (n.contains("giabansi") || (n.contains("bansi") && n.contains("10")) || n.equals("giabansi")) {
            return COL_WHOLESALE;
        }
        if (n.contains("trangthai") || n.equals("status")) {
            return COL_STATUS;
        }
        // Giá gốc / phí ship / lượng hàng — nhận diện để bỏ qua (không map)
        return null;
    }

    /** Chuẩn hóa header: chuyển thường, bỏ dấu, bỏ khoảng trắng thừa. */
    private static String normalizeHeader(String raw) {
        String s = stripAccents(raw.trim().toLowerCase(Locale.ROOT));
        s = s.replace('\n', ' ').replace('\r', ' ');
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    /** Bỏ dấu tiếng Việt (NFD normalization). */
    private static String stripAccents(String input) {
        if (input == null) {
            return "";
        }
        String n = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }

    /** Kiểm tra dòng Excel có trống hoàn toàn (cả mã và tên đều rỗng). */
    private static boolean isBlankRow(Row row, Map<String, Integer> colMap, DataFormatter formatter) {
        String code = cellText(row, colMap.get(COL_CODE), formatter);
        String name = cellText(row, colMap.get(COL_NAME), formatter);
        return (code == null || code.isBlank()) && (name == null || name.isBlank());
    }

    /** Đọc nội dung text từ 1 ô Excel theo index cột. */
    private static String cellText(Row row, Integer idx, DataFormatter formatter) {
        if (row == null || idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            double v = cell.getNumericCellValue();
            if (v == Math.rint(v) && !Double.isInfinite(v)) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
        String text = formatter.formatCellValue(cell);
        return text != null ? text.trim() : null;
    }

    /** Chuẩn hóa mã sản phẩm: xử lý số thập phân Excel (1.0 → 1) và trim. */
    private static String normalizeCode(String raw) {
        String s = trimToNull(raw);
        if (s == null) {
            return null;
        }
        // Excel numeric "1.0" → "1"
        if (s.matches("\\d+\\.0+")) {
            return s.substring(0, s.indexOf('.'));
        }
        return s;
    }

    /** Trim chuỗi và trả về null nếu kết quả rỗng. */
    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Parse VND dạng "11,000 đ" / "11.000 đ" / "11000" → Double.
     */
    static Double parseVnd(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.equals("##") || s.equals("-") || s.equalsIgnoreCase("n/a")) {
            return null;
        }
        s = s.replace("đ", "").replace("Đ", "")
                .replace("VND", "").replace("vnd", "")
                .replace("₫", "")
                .replace(" ", "")
                .trim();
        if (s.isEmpty()) {
            return null;
        }
        // keep digits, comma, dot
        String cleaned = s.replaceAll("[^0-9,.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-")) {
            return null;
        }
        if (cleaned.contains(",") && cleaned.contains(".")) {
            int lastComma = cleaned.lastIndexOf(',');
            int lastDot = cleaned.lastIndexOf('.');
            if (lastComma > lastDot) {
                // 11.000,50
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                // 11,000.50
                cleaned = cleaned.replace(",", "");
            }
        } else if (cleaned.contains(",")) {
            if (cleaned.matches("-?\\d{1,3}(,\\d{3})+")) {
                cleaned = cleaned.replace(",", "");
            } else if (cleaned.matches("-?\\d+,\\d{1,2}")) {
                cleaned = cleaned.replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (cleaned.contains(".")) {
            if (cleaned.matches("-?\\d{1,3}(\\.\\d{3})+")) {
                cleaned = cleaned.replace(".", "");
            }
            // else keep as decimal
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá không hợp lệ: " + raw);
        }
    }

    /** Parse trạng thái active/inactive từ text Excel (hỗ trợ tiếng Việt và tiếng Anh). */
    private static Boolean parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String n = stripAccents(raw.trim().toLowerCase(Locale.ROOT));
        if (n.contains("con hang") || n.contains("dang kinh doanh") || n.equals("active")
                || n.equals("1") || n.equals("true") || n.equals("co")) {
            return true;
        }
        if (n.contains("het hang") || n.contains("ngung") || n.equals("inactive")
                || n.equals("0") || n.equals("false") || n.contains("ngung kinh doanh")) {
            return false;
        }
        return true;
    }
}
