package com.frezo.crm.service.impl;

import com.frezo.crm.dto.ActivityRequest;
import com.frezo.crm.entity.DealActivity;
import com.frezo.crm.repository.DealActivityRepository;
import com.frezo.crm.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final DealActivityRepository repo;

    /**
     * Ghi lại một hoạt động (activity) cho một thương vụ (deal) hoặc khách hàng.
     * Tạo bản ghi DealActivity từ thông tin trong ActivityRequest và lưu vào cơ sở dữ liệu.
     *
     * @param r yêu cầu chứa thông tin hoạt động
     * @return bản ghi DealActivity vừa được lưu
     */
    @Override
    @Transactional
    public DealActivity log(ActivityRequest r) {
        DealActivity a = DealActivity.builder()
                .dealId(r.getDealId())
                .customerId(r.getCustomerId())
                .activityType(r.getActivityType())
                .subject(r.getSubject())
                .content(r.getContent())
                .happenedAt(r.getHappenedAt() != null ? r.getHappenedAt() : LocalDateTime.now())
                .ownerUsername(r.getOwnerUsername())
                .build();
        a.setIsDeleted(false);
        return repo.save(a);
    }

    /**
     * Xóa mềm một hoạt động theo mã định danh.
     * Đánh dấu bản ghi là đã xóa (isDeleted = true) thay vì xóa vật lý.
     *
     * @param id mã định danh của hoạt động cần xóa
     */
    @Override
    @Transactional
    public void delete(String id) {
        repo.findById(id).ifPresent(a -> {
            a.setIsDeleted(true);
            repo.save(a);
        });
    }

    /**
     * Lấy danh sách hoạt động theo mã thương vụ, sắp xếp theo thời gian giảm dần.
     *
     * @param dealId mã thương vụ
     * @return danh sách các hoạt động liên quan đến thương vụ
     */
    @Override
    @Transactional(readOnly = true)
    public List<DealActivity> byDeal(String dealId) {
        return repo.findByDealIdAndIsDeletedFalseOrderByHappenedAtDesc(dealId);
    }

    /**
     * Lấy danh sách hoạt động theo mã khách hàng, sắp xếp theo thời gian giảm dần.
     *
     * @param customerId mã khách hàng
     * @return danh sách các hoạt động liên quan đến khách hàng
     */
    @Override
    @Transactional(readOnly = true)
    public List<DealActivity> byCustomer(String customerId) {
        return repo.findByCustomerIdAndIsDeletedFalseOrderByHappenedAtDesc(customerId);
    }
}
