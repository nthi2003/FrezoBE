package com.frezo.fbautomation.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.entity.Customer;
import com.frezo.customer.repository.CustomerRepository;
import com.frezo.fbautomation.dto.response.FacebookLeadResponse;
import com.frezo.fbautomation.entity.FacebookLead;
import com.frezo.fbautomation.mapper.FacebookLeadMapper;
import com.frezo.fbautomation.repository.FacebookLeadRepository;
import com.frezo.fbautomation.service.FacebookLeadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookLeadServiceImpl implements FacebookLeadService {

    private final FacebookLeadRepository leadRepository;
    private final FacebookLeadMapper leadMapper;
    private final CustomerRepository customerRepository;

    @Override
    public List<FacebookLeadResponse> getAll(String status, String source) {
        boolean noStatus = status == null || status.isBlank() || "all".equalsIgnoreCase(status);
        boolean noSource = source == null || source.isBlank() || "all".equalsIgnoreCase(source);

        List<FacebookLead> leads;
        if (noStatus && noSource) {
            leads = leadRepository.findAll();
        } else if (noStatus) {
            leads = leadRepository.findBySource(source.toUpperCase());
        } else if (noSource) {
            leads = leadRepository.findByStatus(status);
        } else {
            leads = leadRepository.findByStatusAndSource(status, source.toUpperCase());
        }
        return leads.stream()
                .sorted((a, b) -> {
                    // Mới nhất lên đầu — BE thường trả theo createdDate desc cho inbox.
                    if (a.getCreatedDate() == null) return 1;
                    if (b.getCreatedDate() == null) return -1;
                    return b.getCreatedDate().compareTo(a.getCreatedDate());
                })
                .map(leadMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FacebookLeadResponse assign(String id, String username) {
        FacebookLead lead = findById(id);
        lead.setAssignedTo(username);
        if (lead.getStatus() == null || "NEW".equals(lead.getStatus())) {
            lead.setStatus("ASSIGNED");
        }
        FacebookLead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }

    @Override
    public FacebookLeadResponse getById(String id) {
        return leadMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public void delete(String id) {
        findById(id);
        leadRepository.deleteById(id);
    }

    @Override
    @Transactional
    public String importToCustomer(String id) {
        // 1. Lấy lead từ DB
        FacebookLead lead = findById(id);
        if ("IMPORTED".equals(lead.getStatus())) {
            throw new QTHTException("Lead này đã được import trước đó");
        }

        // 2. Tạo CustomerRequest từ lead
        CustomerRequest customerReq = new CustomerRequest();
        customerReq.setName(lead.getName() != null ? lead.getName() : "Khách hàng từ FB");
        customerReq.setPhone(lead.getPhone());
        customerReq.setEmail(lead.getEmail());
        customerReq.setAddress(lead.getAddress());
        customerReq.setType("LEAD_FB");
        customerReq.setStatus("POTENTIAL");
        customerReq.setCategoryCode("KHTN_FB");
        customerReq.setNote("Nguồn: Group " + lead.getSourceGroupName()
                + " | FB Profile: " + lead.getProfileUrl()
                + " | " + (lead.getNote() != null ? lead.getNote() : ""));

        // 3. Sinh mã KH nếu chưa có
        String code;
        do {
            code = SecureCodeGenerator.generateCode("KH");
        } while (customerRepository.existsByCode(code));
        customerReq.setCode(code);

        // 4. Lưu vào bảng customers
        Customer customer = new Customer();
        customer.setName(customerReq.getName());
        customer.setCode(customerReq.getCode());
        customer.setPhone(customerReq.getPhone());
        customer.setEmail(customerReq.getEmail());
        customer.setAddress(customerReq.getAddress());
        customer.setType(customerReq.getType());
        customer.setStatus(customerReq.getStatus());
        customer.setCategoryCode(customerReq.getCategoryCode());
        customer.setNote(customerReq.getNote());
        customer = customerRepository.save(customer);

        // 5. Cập nhật trạng thái lead
        lead.setStatus("IMPORTED");
        lead.setImportedCustomerId(customer.getId());
        leadRepository.save(lead);

        log.info("Đã import lead {} vào customer {} (code={})", lead.getName(), customer.getId(), customer.getCode());
        return customer.getId();
    }

    @Override
    @Transactional
    public int importAllToCustomer(List<String> ids) {
        int count = 0;
        for (String id : ids) {
            try {
                importToCustomer(id);
                count++;
            } catch (Exception e) {
                log.warn("Bỏ qua lead {} do lỗi: {}", id, e.getMessage());
            }
        }
        return count;
    }

    @Override
    public long countByStatus(String status) {
        return leadRepository.countByStatus(status);
    }

    private FacebookLead findById(String id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy lead Facebook"));
    }
}
