package com.frezo.warehouse.service;

import com.frezo.common.exception.QTHTException;
import com.frezo.warehouse.dto.response.GinResponse;
import com.frezo.warehouse.dto.response.GrnResponse;
import com.frezo.warehouse.dto.response.TransferResponse;
import com.frezo.warehouse.service.GoodsIssueNoteService;
import com.frezo.warehouse.service.GoodsReceiptNoteService;
import com.frezo.warehouse.service.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DocumentPrintService {

    private final GoodsReceiptNoteService grnService;
    private final GoodsIssueNoteService ginService;
    private final StockTransferService transferService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String printGrn(String id) {
        GrnResponse grn = grnService.getById(id);
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8">
            <title>Phiếu nhập kho</title>
            <style>
                body { font-family: 'Times New Roman', serif; font-size: 13px; margin: 20px; }
                h1 { text-align: center; font-size: 18px; text-transform: uppercase; margin-bottom: 5px; }
                .sub-title { text-align: center; font-size: 12px; color: #555; margin-bottom: 20px; }
                .info-table { width: 100%%; border-collapse: collapse; margin-bottom: 15px; }
                .info-table td { padding: 3px 5px; }
                .info-table .label { font-weight: bold; width: 120px; }
                .items-table { width: 100%%; border-collapse: collapse; }
                .items-table th, .items-table td { border: 1px solid #333; padding: 5px; text-align: center; }
                .items-table th { background: #f0f0f0; font-weight: bold; }
                .items-table td.left { text-align: left; }
                .total-row td { font-weight: bold; }
                .signature { margin-top: 50px; width: 100%%; }
                .signature td { text-align: center; padding: 0 20px; font-size: 12px; }
                .signature .line { margin-top: 40px; border-top: 1px solid #333; padding-top: 5px; }
            </style>
            </head><body>
            <h1>PHIẾU NHẬP KHO</h1>
            <p class="sub-title">Mã số: """ + grn.getGrnCode() + """
            """).append(" | Ngày: ").append(grn.getCreatedDate() != null ? grn.getCreatedDate().format(DTF) : "").append("</p>");

        html.append("<table class='info-table'>");
        html.append("<tr><td class='label'>Kho:</td><td>").append(nullSafe(grn.getWarehouseId())).append("</td></tr>");
        html.append("<tr><td class='label'>Nhà cung cấp:</td><td>").append(nullSafe(grn.getSupplierId())).append("</td></tr>");
        html.append("<tr><td class='label'>PO:</td><td>").append(nullSafe(grn.getPurchaseOrderId())).append("</td></tr>");
        html.append("<tr><td class='label'>Trạng thái:</td><td>").append(nullSafe(grn.getStatus())).append("</td></tr>");
        html.append("<tr><td class='label'>Người nhập:</td><td>").append(nullSafe(grn.getReceivedBy())).append("</td></tr>");
        html.append("<tr><td class='label'>Ghi chú:</td><td>").append(nullSafe(grn.getNote())).append("</td></tr>");
        html.append("</table>");

        html.append("<table class='items-table'>");
        html.append("<thead><tr><th>STT</th><th>Sản phẩm</th><th>Lô</th><th>SL Dự kiến</th><th>SL Nhập</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");
        int stt = 0;
        double total = 0;
        for (GrnResponse.GrnItemResponse item : grn.getItems()) {
            stt++;
            double lineTotal = (item.getQtyReceived() != null ? item.getQtyReceived() : 0)
                    * (item.getUnitCost() != null ? item.getUnitCost() : 0);
            total += lineTotal;
            html.append("<tr>")
                .append("<td>").append(stt).append("</td>")
                .append("<td class='left'>").append(nullSafe(item.getProductId())).append("</td>")
                .append("<td>").append(nullSafe(item.getBatchId())).append("</td>")
                .append("<td>").append(formatQty(item.getQtyExpected())).append("</td>")
                .append("<td>").append(formatQty(item.getQtyReceived())).append("</td>")
                .append("<td>").append(formatPrice(item.getUnitCost())).append("</td>")
                .append("<td>").append(formatPrice(lineTotal)).append("</td>")
                .append("</tr>");
        }
        html.append("<tr class='total-row'><td colspan='5'></td><td>Tổng:</td><td>").append(formatPrice(total)).append("</td></tr>");
        html.append("</tbody></table>");

        html.append("""
            <table class="signature">
            <tr>
                <td>NGƯỜI LẬP PHIẾU</td>
                <td>NGƯỜI NHẬN HÀNG</td>
                <td>THỦ KHO</td>
                <td>KẾ TOÁN</td>
            </tr>
            <tr>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
            </tr>
            </table>
            </body></html>""");

        return html.toString();
    }

    public String printGin(String id) {
        GinResponse gin = ginService.getById(id);
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8">
            <title>Phiếu xuất kho</title>
            <style>
                body { font-family: 'Times New Roman', serif; font-size: 13px; margin: 20px; }
                h1 { text-align: center; font-size: 18px; text-transform: uppercase; margin-bottom: 5px; }
                .sub-title { text-align: center; font-size: 12px; color: #555; margin-bottom: 20px; }
                .info-table { width: 100%%; border-collapse: collapse; margin-bottom: 15px; }
                .info-table td { padding: 3px 5px; }
                .info-table .label { font-weight: bold; width: 120px; }
                .items-table { width: 100%%; border-collapse: collapse; }
                .items-table th, .items-table td { border: 1px solid #333; padding: 5px; text-align: center; }
                .items-table th { background: #f0f0f0; font-weight: bold; }
                .items-table td.left { text-align: left; }
                .total-row td { font-weight: bold; }
                .signature { margin-top: 50px; width: 100%%; }
                .signature td { text-align: center; padding: 0 20px; font-size: 12px; }
                .signature .line { margin-top: 40px; border-top: 1px solid #333; padding-top: 5px; }
            </style>
            </head><body>
            <h1>PHIẾU XUẤT KHO</h1>
            <p class="sub-title">Mã số: """ + gin.getGinCode() + """
            """).append(" | Ngày: ").append(gin.getCreatedDate() != null ? gin.getCreatedDate().format(DTF) : "").append("</p>");

        html.append("<table class='info-table'>");
        html.append("<tr><td class='label'>Kho:</td><td>").append(nullSafe(gin.getWarehouseId())).append("</td></tr>");
        html.append("<tr><td class='label'>Khách hàng:</td><td>").append(nullSafe(gin.getCustomerId())).append("</td></tr>");
        html.append("<tr><td class='label'>Đơn hàng:</td><td>").append(nullSafe(gin.getOrderId())).append("</td></tr>");
        html.append("<tr><td class='label'>Loại xuất:</td><td>").append(nullSafe(gin.getIssueType())).append("</td></tr>");
        html.append("<tr><td class='label'>Trạng thái:</td><td>").append(nullSafe(gin.getStatus())).append("</td></tr>");
        html.append("<tr><td class='label'>Người xuất:</td><td>").append(nullSafe(gin.getIssuedBy())).append("</td></tr>");
        html.append("<tr><td class='label'>Ghi chú:</td><td>").append(nullSafe(gin.getNote())).append("</td></tr>");
        html.append("</table>");

        html.append("<table class='items-table'>");
        html.append("<thead><tr><th>STT</th><th>Sản phẩm</th><th>Lô</th><th>SL Yêu cầu</th><th>SL Xuất</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");
        int stt = 0;
        double total = 0;
        for (GinResponse.GinItemResponse item : gin.getItems()) {
            stt++;
            double lineTotal = (item.getQtyIssued() != null ? item.getQtyIssued() : 0)
                    * (item.getUnitCost() != null ? item.getUnitCost() : 0);
            total += lineTotal;
            html.append("<tr>")
                .append("<td>").append(stt).append("</td>")
                .append("<td class='left'>").append(nullSafe(item.getProductId())).append("</td>")
                .append("<td>").append(nullSafe(item.getBatchId())).append("</td>")
                .append("<td>").append(formatQty(item.getQtyRequested())).append("</td>")
                .append("<td>").append(formatQty(item.getQtyIssued())).append("</td>")
                .append("<td>").append(formatPrice(item.getUnitCost())).append("</td>")
                .append("<td>").append(formatPrice(lineTotal)).append("</td>")
                .append("</tr>");
        }
        html.append("<tr class='total-row'><td colspan='5'></td><td>Tổng:</td><td>").append(formatPrice(total)).append("</td></tr>");
        html.append("</tbody></table>");

        html.append("""
            <table class="signature">
            <tr>
                <td>NGƯỜI LẬP PHIẾU</td>
                <td>NGƯỜI NHẬN HÀNG</td>
                <td>THỦ KHO</td>
                <td>KẾ TOÁN</td>
            </tr>
            <tr>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
            </tr>
            </table>
            </body></html>""");

        return html.toString();
    }

    public String printTransfer(String id) {
        TransferResponse transfer = transferService.getById(id);
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8">
            <title>Phiếu chuyển kho</title>
            <style>
                body { font-family: 'Times New Roman', serif; font-size: 13px; margin: 20px; }
                h1 { text-align: center; font-size: 18px; text-transform: uppercase; margin-bottom: 5px; }
                .sub-title { text-align: center; font-size: 12px; color: #555; margin-bottom: 20px; }
                .info-table { width: 100%%; border-collapse: collapse; margin-bottom: 15px; }
                .info-table td { padding: 3px 5px; }
                .info-table .label { font-weight: bold; width: 140px; }
                .items-table { width: 100%%; border-collapse: collapse; }
                .items-table th, .items-table td { border: 1px solid #333; padding: 5px; text-align: center; }
                .items-table th { background: #f0f0f0; font-weight: bold; }
                .items-table td.left { text-align: left; }
                .total-row td { font-weight: bold; }
                .signature { margin-top: 50px; width: 100%%; }
                .signature td { text-align: center; padding: 0 20px; font-size: 12px; }
                .signature .line { margin-top: 40px; border-top: 1px solid #333; padding-top: 5px; }
            </style>
            </head><body>
            <h1>PHIẾU CHUYỂN KHO</h1>
            <p class="sub-title">Mã số: """ + transfer.getTransferCode() + """
            """).append(" | Ngày: ").append(transfer.getCreatedDate() != null ? transfer.getCreatedDate().format(DTF) : "").append("</p>");

        html.append("<table class='info-table'>");
        html.append("<tr><td class='label'>Kho nguồn:</td><td>").append(nullSafe(transfer.getFromWarehouseId())).append("</td></tr>");
        html.append("<tr><td class='label'>Kho đích:</td><td>").append(nullSafe(transfer.getToWarehouseId())).append("</td></tr>");
        html.append("<tr><td class='label'>Trạng thái:</td><td>").append(nullSafe(transfer.getStatus())).append("</td></tr>");
        html.append("<tr><td class='label'>Người chuyển:</td><td>").append(nullSafe(transfer.getTransferredBy())).append("</td></tr>");
        html.append("<tr><td class='label'>Ghi chú:</td><td>").append(nullSafe(transfer.getNote())).append("</td></tr>");
        html.append("</table>");

        html.append("<table class='items-table'>");
        html.append("<thead><tr><th>STT</th><th>Sản phẩm</th><th>Lô</th><th>SL Chuyển</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");
        int stt = 0;
        double total = 0;
        for (TransferResponse.TransferItemResponse item : transfer.getItems()) {
            stt++;
            double lineTotal = (item.getQtyTransferred() != null ? item.getQtyTransferred() : 0)
                    * (item.getUnitCost() != null ? item.getUnitCost() : 0);
            total += lineTotal;
            html.append("<tr>")
                .append("<td>").append(stt).append("</td>")
                .append("<td class='left'>").append(nullSafe(item.getProductId())).append("</td>")
                .append("<td>").append(nullSafe(item.getBatchId())).append("</td>")
                .append("<td>").append(formatQty(item.getQtyTransferred())).append("</td>")
                .append("<td>").append(formatPrice(item.getUnitCost())).append("</td>")
                .append("<td>").append(formatPrice(lineTotal)).append("</td>")
                .append("</tr>");
        }
        html.append("<tr class='total-row'><td colspan='4'></td><td>Tổng:</td><td>").append(formatPrice(total)).append("</td></tr>");
        html.append("</tbody></table>");

        html.append("""
            <table class="signature">
            <tr>
                <td>NGƯỜI LẬP PHIẾU</td>
                <td>KẾ TOÁN</td>
                <td>THỦ KHO XUẤT</td>
                <td>THỦ KHO NHẬP</td>
            </tr>
            <tr>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
                <td class="line"></td>
            </tr>
            </table>
            </body></html>""");

        return html.toString();
    }

    public byte[] exportGrnExcel(String id) {
        GrnResponse grn = grnService.getById(id);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Phieu nhap kho");
            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            CellStyle borderStyle = wb.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            int r = 0;
            Row title = sheet.createRow(r++);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("PHIẾU NHẬP KHO");
            CellStyle ts = wb.createCellStyle();
            Font tf = wb.createFont();
            tf.setBold(true);
            tf.setFontHeightInPoints((short) 16);
            ts.setFont(tf);
            titleCell.setCellStyle(ts);

            r++;
            sheet.createRow(r++).createCell(0).setCellValue("Mã: " + grn.getGrnCode());
            sheet.createRow(r++).createCell(0).setCellValue("Kho: " + nullSafe(grn.getWarehouseId()));
            sheet.createRow(r++).createCell(0).setCellValue("Nhà cung cấp: " + nullSafe(grn.getSupplierId()));
            sheet.createRow(r++).createCell(0).setCellValue("Ngày: " + (grn.getCreatedDate() != null ? grn.getCreatedDate().format(DTF) : ""));
            sheet.createRow(r++).createCell(0).setCellValue("Người nhập: " + nullSafe(grn.getReceivedBy()));
            sheet.createRow(r++).createCell(0).setCellValue("Ghi chú: " + nullSafe(grn.getNote()));
            r++;

            Row header = sheet.createRow(r++);
            String[] cols = {"STT", "Sản phẩm", "Lô", "SL Dự kiến", "SL Nhập", "Đơn giá", "Thành tiền"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(boldStyle);
            }

            int stt = 0;
            double total = 0;
            for (GrnResponse.GrnItemResponse item : grn.getItems()) {
                stt++;
                Row row = sheet.createRow(r++);
                double lineTotal = (item.getQtyReceived() != null ? item.getQtyReceived() : 0)
                        * (item.getUnitCost() != null ? item.getUnitCost() : 0);
                total += lineTotal;
                row.createCell(0).setCellValue(stt);
                row.createCell(1).setCellValue(nullSafe(item.getProductId()));
                row.createCell(2).setCellValue(nullSafe(item.getBatchId()));
                row.createCell(3).setCellValue(item.getQtyExpected() != null ? item.getQtyExpected() : 0);
                row.createCell(4).setCellValue(item.getQtyReceived() != null ? item.getQtyReceived() : 0);
                row.createCell(5).setCellValue(item.getUnitCost() != null ? item.getUnitCost() : 0);
                row.createCell(6).setCellValue(lineTotal);
            }

            Row totalRow = sheet.createRow(r++);
            totalRow.createCell(5).setCellValue("Tổng:");
            totalRow.getCell(5).setCellStyle(boldStyle);
            totalRow.createCell(6).setCellValue(total);

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new QTHTException("stock.export.failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] exportGinExcel(String id) {
        GinResponse gin = ginService.getById(id);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Phieu xuat kho");
            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            int r = 0;
            Row title = sheet.createRow(r++);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("PHIẾU XUẤT KHO");
            CellStyle ts = wb.createCellStyle();
            Font tf = wb.createFont();
            tf.setBold(true);
            tf.setFontHeightInPoints((short) 16);
            ts.setFont(tf);
            titleCell.setCellStyle(ts);

            r++;
            sheet.createRow(r++).createCell(0).setCellValue("Mã: " + gin.getGinCode());
            sheet.createRow(r++).createCell(0).setCellValue("Kho: " + nullSafe(gin.getWarehouseId()));
            sheet.createRow(r++).createCell(0).setCellValue("Khách hàng: " + nullSafe(gin.getCustomerId()));
            sheet.createRow(r++).createCell(0).setCellValue("Ngày: " + (gin.getCreatedDate() != null ? gin.getCreatedDate().format(DTF) : ""));
            sheet.createRow(r++).createCell(0).setCellValue("Người xuất: " + nullSafe(gin.getIssuedBy()));
            sheet.createRow(r++).createCell(0).setCellValue("Ghi chú: " + nullSafe(gin.getNote()));
            r++;

            Row header = sheet.createRow(r++);
            String[] cols = {"STT", "Sản phẩm", "Lô", "SL Yêu cầu", "SL Xuất", "Đơn giá", "Thành tiền"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(boldStyle);
            }

            int stt = 0;
            double total = 0;
            for (GinResponse.GinItemResponse item : gin.getItems()) {
                stt++;
                Row row = sheet.createRow(r++);
                double lineTotal = (item.getQtyIssued() != null ? item.getQtyIssued() : 0)
                        * (item.getUnitCost() != null ? item.getUnitCost() : 0);
                total += lineTotal;
                row.createCell(0).setCellValue(stt);
                row.createCell(1).setCellValue(nullSafe(item.getProductId()));
                row.createCell(2).setCellValue(nullSafe(item.getBatchId()));
                row.createCell(3).setCellValue(item.getQtyRequested() != null ? item.getQtyRequested() : 0);
                row.createCell(4).setCellValue(item.getQtyIssued() != null ? item.getQtyIssued() : 0);
                row.createCell(5).setCellValue(item.getUnitCost() != null ? item.getUnitCost() : 0);
                row.createCell(6).setCellValue(lineTotal);
            }

            Row totalRow = sheet.createRow(r++);
            totalRow.createCell(5).setCellValue("Tổng:");
            totalRow.getCell(5).setCellStyle(boldStyle);
            totalRow.createCell(6).setCellValue(total);

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new QTHTException("stock.export.failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }

    private String formatQty(Double qty) {
        if (qty == null) return "0";
        if (qty == Math.floor(qty)) return String.valueOf((int) Math.floor(qty));
        return String.format("%.2f", qty);
    }

    private String formatPrice(Double price) {
        if (price == null) return "0";
        return String.format("%,.0f", price);
    }
}
