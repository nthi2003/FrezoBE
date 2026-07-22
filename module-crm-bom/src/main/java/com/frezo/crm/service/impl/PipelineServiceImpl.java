package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.dto.PipelineRequest;
import com.frezo.crm.entity.Pipeline;
import com.frezo.crm.entity.Stage;
import com.frezo.crm.repository.PipelineRepository;
import com.frezo.crm.repository.StageRepository;
import com.frezo.crm.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository pipelineRepo;
    private final StageRepository stageRepo;

    @Override
    @Transactional
    public Pipeline create(PipelineRequest r) {
        Pipeline p = Pipeline.builder()
                .name(r.getName())
                .description(r.getDescription())
                .isDefault(Boolean.TRUE.equals(r.getIsDefault()))
                .active(r.getActive() != null ? r.getActive() : true)
                .build();
        p.setIsDeleted(false);
        Pipeline saved = pipelineRepo.save(p);
        if (r.getStages() != null) {
            for (PipelineRequest.StageInline s : r.getStages()) {
                saveStage(saved.getId(), s);
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public Pipeline update(String id, PipelineRequest r) {
        Pipeline p = get(id);
        p.setName(r.getName());
        p.setDescription(r.getDescription());
        if (r.getIsDefault() != null) p.setIsDefault(r.getIsDefault());
        if (r.getActive() != null) p.setActive(r.getActive());
        pipelineRepo.save(p);
        if (r.getStages() != null) {
            for (PipelineRequest.StageInline s : r.getStages()) saveStage(id, s);
        }
        return p;
    }

    @Override
    @Transactional
    public void delete(String id) {
        Pipeline p = get(id);
        p.setIsDeleted(true);
        pipelineRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Pipeline get(String id) {
        return pipelineRepo.findById(id)
                .orElseThrow(() -> new AppException(CrmErrorCode.PIPELINE_NOT_FOUND, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pipeline> list() {
        return pipelineRepo.findByIsDeletedFalseOrderByCreatedDateAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Stage> stages(String pipelineId) {
        return stageRepo.findByPipelineIdAndIsDeletedFalseOrderByOrderNoAsc(pipelineId);
    }

    @Override
    @Transactional
    public Pipeline ensureDefault() {
        return pipelineRepo.findFirstByIsDefaultTrueAndIsDeletedFalse().orElseGet(() -> {
            Pipeline p = Pipeline.builder()
                    .name("Sales Pipeline mặc định")
                    .description("Tạo tự động khi khởi tạo module CRM")
                    .isDefault(true)
                    .active(true)
                    .build();
            p.setIsDeleted(false);
            pipelineRepo.save(p);

            // 5 stage chuẩn
            createSeedStage(p.getId(), "Tiềm năng", 0, 10, null);
            createSeedStage(p.getId(), "Đủ điều kiện", 1, 30, null);
            createSeedStage(p.getId(), "Đề xuất / Báo giá", 2, 60, null);
            createSeedStage(p.getId(), "Đàm phán", 3, 80, null);
            createSeedStage(p.getId(), "Chốt Won", 4, 100, true);
            createSeedStage(p.getId(), "Mất Lost", 5, 0, false);
            return p;
        });
    }

    private void saveStage(String pipelineId, PipelineRequest.StageInline s) {
        Stage entity;
        if (s.getId() != null) {
            entity = stageRepo.findById(s.getId()).orElseGet(() -> new Stage());
        } else {
            entity = new Stage();
        }
        entity.setPipelineId(pipelineId);
        entity.setName(s.getName());
        entity.setOrderNo(s.getOrderNo() != null ? s.getOrderNo() : 0);
        entity.setProbability(s.getProbability());
        entity.setWon(s.getWon());
        if (entity.getIsDeleted() == null) entity.setIsDeleted(false);
        stageRepo.save(entity);
    }

    private void createSeedStage(String pipelineId, String name, int order, int prob, Boolean won) {
        Stage s = Stage.builder()
                .pipelineId(pipelineId)
                .name(name)
                .orderNo(order)
                .probability(prob)
                .won(won)
                .build();
        s.setIsDeleted(false);
        stageRepo.save(s);
    }
}
