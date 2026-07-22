package com.frezo.warehouse.listener;

import com.frezo.approval.event.ApprovalDecidedEvent;
import com.frezo.common.domain.SubjectType;
import com.frezo.common.service.NotificationService;
import com.frezo.warehouse.entity.PurchaseRequest;
import com.frezo.warehouse.repository.PurchaseRequestLineRepository;
import com.frezo.warehouse.repository.PurchaseRequestRepository;
import com.frezo.warehouse.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseRequestApprovalListener {

    private final PurchaseRequestRepository prRepository;
    private final PurchaseRequestLineRepository lineRepository;
    private final StockAlertRepository alertRepository;
    private final NotificationService notificationService;

    @EventListener
    @Transactional
    public void onDecided(ApprovalDecidedEvent event) {
        if (!SubjectType.PURCHASE_REQUEST.name().equals(event.getSubjectType())) return;
        PurchaseRequest pr = prRepository.findById(event.getSubjectId()).orElse(null);
        if (pr == null) return;
        String link = "/warehouse/purchase-requests?id=" + pr.getId();
        if ("APPROVED".equals(event.getStatus())) {
            pr.setStatus("APPROVED");
            lineRepository.findByPurchaseRequestIdAndIsDeletedFalse(pr.getId()).forEach(l -> {
                if (l.getStockAlertId() != null) {
                    alertRepository.findById(l.getStockAlertId()).ifPresent(a -> {
                        a.setStatus("RESOLVED");
                        alertRepository.save(a);
                    });
                }
            });
            if (pr.getCreatedBy() != null) {
                notificationService.notify(
                        pr.getCreatedBy(),
                        "PR đã được duyệt",
                        pr.getCode() != null ? pr.getCode() : pr.getId(),
                        "PR_APPROVED",
                        "PURCHASE_REQUEST",
                        pr.getId(),
                        link,
                        event.getActedBy(),
                        false);
            }
        } else if ("REJECTED".equals(event.getStatus())) {
            pr.setStatus("REJECTED");
            lineRepository.findByPurchaseRequestIdAndIsDeletedFalse(pr.getId()).forEach(l -> {
                if (l.getStockAlertId() != null) {
                    alertRepository.findById(l.getStockAlertId()).ifPresent(a -> {
                        a.setPurchaseRequestId(null);
                        alertRepository.save(a);
                    });
                }
            });
            pr.setApprovalRequestId(null);
            if (pr.getCreatedBy() != null) {
                notificationService.notify(
                        pr.getCreatedBy(),
                        "PR bị từ chối",
                        pr.getCode() != null ? pr.getCode() : pr.getId(),
                        "PR_REJECTED",
                        "PURCHASE_REQUEST",
                        pr.getId(),
                        link,
                        event.getActedBy(),
                        false);
            }
        }
        prRepository.save(pr);
        log.info("[PR] Sync {} → {}", pr.getId(), pr.getStatus());
    }
}
