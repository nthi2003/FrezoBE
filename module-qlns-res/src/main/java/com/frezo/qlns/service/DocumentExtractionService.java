package com.frezo.qlns.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionService {

    private final Tika tika = new Tika();
    private final AiExtractionService aiExtractionService;

    public Map<String, String> extractFields(MultipartFile file) {
        Map<String, Object> aiResult = aiExtractionService.extract(file);
        if (aiResult.containsKey("error")) {
            log.warn("AI extraction failed, falling back to Tika: {}", aiResult.get("error"));
            String text = extractTextWithTika(file);
            return parseFields(text);
        }
        Map<String, String> fields = new HashMap<>();
        aiResult.forEach((key, value) -> {
            if (value instanceof String) {
                fields.put(key, (String) value);
            }
        });
        return fields;
    }

    public String extractTextWithTika(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.parseToString(inputStream);
        } catch (IOException | TikaException e) {
            log.error("Lỗi khi trích xuất nội dung từ file: ", e);
            throw new RuntimeException("Không thể trích xuất nội dung từ file: " + file.getOriginalFilename());
        }
    }

    public Map<String, String> parseFields(String text) {
        Map<String, String> fields = new HashMap<>();
        fields.put("content", text);
        fields.put("code", extractByPatterns(text, new String[]{
                "Số[\\s]*:[\\s]*\\.*\\.*\\.*[\\s]*([^\\n\\r]+)",
                "Số hợp đồng[\\s:]*([^\\n\\r]+)",
                "Mã hợp đồng[\\s:]*([^\\n\\r]+)",
                "Contract[\\s]*Code[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("type", extractByPatterns(text, new String[]{
                "HỢP ĐỒNG\\s+([^\\n]+)",
                "Loại hợp đồng[\\s:]*([^\\n\\r]+)",
                "Hình thức hợp đồng[\\s:]*([^\\n\\r]+)",
                "Contract[\\s]*Type[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("personName", extractByPatterns(text, new String[]{
                "Họ và tên[\\s:]*([^\\n\\r]+)",
                "Nhân sự[\\s:]*([^\\n\\r]+)",
                "Người lao động[\\s:]*([^\\n\\r]+)",
                "Employee[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("startDate", extractByPatterns(text, new String[]{
                "kể từ ngày[\\s]*([^\\n\\r]+)",
                "Ngày bắt đầu[\\s:]*([^\\n\\r]+)",
                "Bắt đầu từ[\\s:]*([^\\n\\r]+)",
                "Có hiệu lực từ[\\s:]*([^\\n\\r]+)",
                "Start[\\s]*Date[\\s:]*([^\\n\\r]+)",
                "Effective[\\s]*Date[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("endDate", extractByPatterns(text, new String[]{
                "đến hết ngày[\\s]*([^\\n\\r]+)",
                "Ngày kết thúc[\\s:]*([^\\n\\r]+)",
                "Kết thúc vào[\\s:]*([^\\n\\r]+)",
                "Hết hạn ngày[\\s:]*([^\\n\\r]+)",
                "End[\\s]*Date[\\s:]*([^\\n\\r]+)",
                "Expiry[\\s]*Date[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("employerName", extractByPatterns(text, new String[]{
                "Tên công ty[\\s/đơn vị]*[\\s:]*([^\\n\\r]+)",
                "Tên doanh nghiệp[\\s:]*([^\\n\\r]+)",
                "Employer[\\s]*Name[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("employerAddress", extractByPatterns(text, new String[]{
                "Địa chỉ[\\s]*[^:]*:[\\s]*([^\\n\\r]+)"
        }));
        fields.put("employerTaxCode", extractByPatterns(text, new String[]{
                "Mã số thuế[\\s:]*([\\d\\-]+)",
                "Tax[\\s]*Code[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("employeeIdNumber", extractByPatterns(text, new String[]{
                "CCCD[\\s/]*Hộ chiếu[\\s]*số[\\s:]*([^\\n\\r]+)",
                "CCCD[\\s:]*([^\\n\\r]+)",
                "Hộ chiếu[\\s]*số[\\s:]*([^\\n\\r]+)",
                "Căn cước[\\s:]*([^\\n\\r]+)",
                "ID[\\s]*Number[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("employeeDob", extractByPatterns(text, new String[]{
                "Sinh ngày[\\s:]*([^\\n\\r]+)",
                "Ngày sinh[\\s:]*([^\\n\\r]+)",
                "Date[\\s]*of[\\s]*Birth[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("jobPosition", extractByPatterns(text, new String[]{
                "vị trí[/chức danh]*[\\s:]*([^\\n\\r]+)",
                "Chức danh[\\s:]*([^\\n\\r]+)",
                "Vị trí công việc[\\s:]*([^\\n\\r]+)",
                "Job[\\s]*Position[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("workLocation", extractByPatterns(text, new String[]{
                "Địa điểm làm việc[\\s:]*([^\\n\\r]+)",
                "Nơi làm việc[\\s:]*([^\\n\\r]+)",
                "Work[\\s]*Location[\\s:]*([^\\n\\r]+)"
        }));
        fields.put("probationDays", extractByPatterns(text, new String[]{
                "Thời hạn thử việc[\\s]*là[\\s]*([\\d]+)\\s*ngày",
                "Probation[\\s]*Period[\\s:]*([\\d]+)\\s*days?"
        }));
        fields.put("allowance", extractByPatterns(text, new String[]{
                "Phụ cấp[\\s:]*([^\\n\\r]+)",
                "Allowance[\\s:]*([^\\n\\r]+)"
        }));
        return fields;
    }

    private String extractByPatterns(String text, String[] patterns) {
        for (String regex : patterns) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }
}
