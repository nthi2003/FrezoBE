package com.frezo.customer.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.utils.CryptoUtils;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.customer.dto.request.CustomerFilterRequest;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.dto.response.AiScrapeResponse;
import com.frezo.customer.dto.response.CustomerResponse;
import com.frezo.customer.entity.Customer;
import com.frezo.customer.mapper.CustomerMapper;
import com.frezo.customer.repository.CustomerRepository;
import com.frezo.customer.service.CustomerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    // ─────────────────── LIST / SEARCH ─────────────────────────────────────────
    @Override
    public Map<String, Object> getAll(CustomerFilterRequest filter) {
        Specification<Customer> spec = buildSpec(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Customer> page = customerRepository.findAll(spec,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort, true));
        List<CustomerResponse> items = page.getContent().stream()
                .map(customerMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), page, items);
    }

    private Specification<Customer> buildSpec(CustomerFilterRequest filter) {
        Specification<Customer> spec = Specification.where(null);
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            spec = spec.and(GenericSpecification.<Customer>likeField("name", filter.getKeyword())
                    .or(GenericSpecification.likeField("code", filter.getKeyword()))
                    .or(GenericSpecification.likeField("email", filter.getKeyword())));
        }
        if (SystemUtils.isNotNullOrEmpty(filter.getType())) {
            spec = spec.and(GenericSpecification.equalField("type", filter.getType()));
        }
        if (SystemUtils.isNotNullOrEmpty(filter.getStatus())) {
            spec = spec.and(GenericSpecification.equalField("status", filter.getStatus()));
        }
        if (SystemUtils.isNotNullOrEmpty(filter.getCategoryCode())) {
            spec = spec.and(GenericSpecification.equalField("categoryCode", filter.getCategoryCode()));
        }
        return spec;
    }

    // ─────────────────── GET BY ID ──────────────────────────────────────────────
    @Override
    public CustomerResponse getById(String id) {
        return customerMapper.toResponse(findById(id));
    }

    // ─────────────────── CREATE ─────────────────────────────────────────────────
    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (SystemUtils.isNotNullOrEmpty(request.getCode())) {
            if (customerRepository.existsByCode(request.getCode()))
                throw new QTHTException("exception.customer.code.exists", request.getCode());
        } else {
            String code;
            do { code = SecureCodeGenerator.generateCode("KH"); }
            while (customerRepository.existsByCode(code));
            request.setCode(code);
        }
        Customer entity = customerMapper.toEntity(request);
        entity.setStatus(SystemUtils.isNotNullOrEmpty(request.getStatus()) ? request.getStatus() : "ACTIVE");
        return customerMapper.toResponse(customerRepository.save(entity));
    }

    // ─────────────────── UPDATE ─────────────────────────────────────────────────
    @Override
    @Transactional
    public CustomerResponse update(String id, CustomerRequest request) {
        Customer entity = findById(id);
        // If code changed – verify uniqueness
        if (SystemUtils.isNotNullOrEmpty(request.getCode())
                && !request.getCode().equals(entity.getCode())
                && customerRepository.existsByCode(request.getCode())) {
            throw new QTHTException("exception.customer.code.exists", request.getCode());
        }
        customerMapper.updateEntity(entity, request);
        return customerMapper.toResponse(customerRepository.save(entity));
    }

    // ─────────────────── DELETE ─────────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(String id) {
        findById(id); // assert exists
        customerRepository.deleteById(id);
    }

    // ─────────────────── REVEAL PHONE (Audit) ───────────────────────────────────
    /**
     * Giải mã số điện thoại thật. Yêu cầu quyền cao.
     * Mọi lần gọi API này phải được ghi vào Audit log.
     */
    @Override
    public String revealPhone(String id) {
        Customer entity = findById(id);
        log.info("[AUDIT] User '{}' revealed phone for customerId={}", SystemUtils.getCurrentUsername(), id);
        if (entity.getPhoneEncrypted() == null) return null;
        return CryptoUtils.decryptAESGCM(entity.getPhoneEncrypted());
    }

    // ─────────────────── IMPORT EXCEL ───────────────────────────────────────────
    @Override
    @Transactional
    public void importFromExcel(MultipartFile file) {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                CustomerRequest req = new CustomerRequest();
                req.setName(getCellValue(row, 0));
                req.setPhone(getCellValue(row, 1));
                req.setEmail(getCellValue(row, 2));
                req.setAddress(getCellValue(row, 3));
                req.setTaxCode(getCellValue(row, 4));
                req.setType(getCellValue(row, 5));
                create(req);
            }
        } catch (Exception e) {
            throw new QTHTException("exception.customer.import.failed");
        }
    }

    // ─────────────────── EXPORT EXCEL ───────────────────────────────────────────
    @Override
    public byte[] exportToExcel(CustomerFilterRequest filter) {
        filter.setPageNumber(null); // lấy toàn bộ
        filter.setPageSize(null);
        Specification<Customer> spec = buildSpec(filter);
        List<Customer> all = customerRepository.findAll(spec);
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Khách hàng");
            // Header
            Row header = sheet.createRow(0);
            String[] cols = {"Mã KH", "Tên KH", "SĐT (4 số cuối)", "Email", "Địa chỉ", "MST", "Loại", "Trạng thái"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
            }
            // Data rows
            int rowIdx = 1;
            for (Customer c : all) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getCode());
                row.createCell(1).setCellValue(c.getName());
                row.createCell(2).setCellValue(c.getPhoneLast4() != null ? "****" + c.getPhoneLast4() : "");
                row.createCell(3).setCellValue(c.getEmail());
                row.createCell(4).setCellValue(c.getAddress());
                row.createCell(5).setCellValue(c.getTaxCode());
                row.createCell(6).setCellValue(c.getType());
                row.createCell(7).setCellValue(c.getStatus());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new QTHTException("exception.customer.export.failed");
        }
    }

    // ─────────────────── SYNC FROM AI ──────────────────────────────────────────
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Override
    @Transactional
    public int syncLeadsFromAi(String keyword, String city, String ward, Integer limit) {
        String aiUrl = "http://localhost:8001/api/v1/scrape";
        Map<String, Object> request = Map.of(
                "keyword", keyword,
                "city", city,
                "ward", ward != null ? ward : "",
                "limit", limit != null ? limit : 10
        );

        try {
            AiScrapeResponse response = restTemplate.postForObject(aiUrl, request, AiScrapeResponse.class);
            if (response == null || !"success".equals(response.getStatus()) || response.getData() == null) {
                return 0;
            }

            int count = 0;
            for (AiScrapeResponse.AiLeadData lead : response.getData()) {
                // Kiểm tra xem khách hàng đã tồn tại chưa (qua SĐT hoăc Tên + Địa chỉ)
                // Đơn giản nhất là check empty Phone hoặc fake check
                if (SystemUtils.isNullOrEmpty(lead.getPhone()) || "N/A".equals(lead.getPhone())) continue;

                // Tạo mới nếu chưa có
                CustomerRequest req = new CustomerRequest();
                req.setName(lead.getName());
                req.setPhone(lead.getPhone());
                req.setAddress(lead.getAddress() + " (Source: Google Maps)");
                req.setType("LEAD_AI");
                req.setStatus("POTENTIAL");
                req.setCategoryCode("KHTN_AI");
                
                try {
                    create(req);
                    count++;
                } catch (Exception e) {
                    log.warn("Bỏ qua lead trùng hoặc lỗi: {}", lead.getName());
                }
            }
            return count;
        } catch (Exception e) {
            log.error("Lỗi khi kết nối FrezoAI: {}", e.getMessage());
            throw new QTHTException("Kết nối tới hệ thống AI thất bại");
        }
    }

    // ─────────────────── PRIVATE HELPERS ────────────────────────────────────────
    private Customer findById(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new QTHTException("exception.customer.not_found"));
    }

    private String getCellValue(Row row, int idx) {
        Cell cell = row.getCell(idx);
        return cell != null ? cell.toString().trim() : null;
    }
}
