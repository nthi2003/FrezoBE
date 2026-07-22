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

    @Override
    @Transactional
    public void delete(String id) {
        repo.findById(id).ifPresent(a -> {
            a.setIsDeleted(true);
            repo.save(a);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealActivity> byDeal(String dealId) {
        return repo.findByDealIdAndIsDeletedFalseOrderByHappenedAtDesc(dealId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DealActivity> byCustomer(String customerId) {
        return repo.findByCustomerIdAndIsDeletedFalseOrderByHappenedAtDesc(customerId);
    }
}
