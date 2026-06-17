package com.frezo.qlns.service.Impl;

import com.frezo.common.constant.WebSocketChannels;
import com.frezo.common.service.NotificationService;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.dto.request.*;
import com.frezo.qlns.dto.response.ContractAsginWorkResponse;
import com.frezo.qlns.dto.response.ContractComboboxResponse;
import com.frezo.qlns.dto.response.ContractResponse;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.ContractAssginWork;
import com.frezo.qlns.entity.ContractHistory;
import com.frezo.qlns.mapper.ContractAssiginWorkMapper;
import com.frezo.qlns.mapper.ContractMapper;
import com.frezo.qlns.repository.ContractAssginWorkReposirory;
import com.frezo.qlns.repository.ContractHisRepository;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.service.ContractService;
import com.frezo.qlns.service.ContractVersionService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractMapper contractMapper;
    private final ContractAssiginWorkMapper contractAssiginWorkMapper;
    private final ContractAssginWorkReposirory contractAssginWorkReposirory;
    private final ContractHisRepository contractHistoryRepository;
    private final ContractVersionService contractVersionService;
    private final NotificationService notificationService;
    private final ContractRepository contractRepository;

    public Map<String, Object> all(ContractFilter filter) {
        Specification<Contract> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Contract> entities = contractRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        List<com.frezo.qlns.dto.response.ContractResponse> responses = entities.getContent().stream()
                .map(contractMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, responses);
    }

    private Specification<Contract> createSpecification(ContractFilter filter) {
        Specification<Contract> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Contract>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("code", filter.getKeyword())));
        }
        if (filter.getEffTo() != null) {
            specification = specification.and(GenericSpecification.equalField("effTo", filter.getEffTo()));
        }
        if (filter.getEffFrom() != null) {
            specification = specification.and(GenericSpecification.equalField("effFrom", filter.getEffFrom()));
        }
        return specification;
    }

    public Response<com.frezo.qlns.dto.response.ContractResponse> add(ContractAddRequest request) {
        validateRequest(request);
        Contract contract = contractMapper.toEntity(request);
        contract.setIsDeleted(false);
        Contract savedContract = contractRepository.save(contract);

        ContractHistory contractHistory = new ContractHistory();
        contractHistory.setContractId(savedContract.getId());
        contractHistoryRepository.save(contractHistory);
        contractVersionService.createVersion(savedContract, "CONTRACT_DONE", "Hồ sơ đã được tạo thành công");

        return Response.ok(contractMapper.toResponse(savedContract));
    }

    public Response<List<com.frezo.qlns.dto.response.ContractComboboxResponse>> combobox(ContractFilter filter) {
        Specification<Contract> specification = createSpecification(filter);
        if (!Boolean.TRUE.equals(filter.getIsDelete())) {
            specification = specification.and(GenericSpecification.hasFieldIs("activated", Boolean.TRUE));
        }
        List<Contract> contracts = contractRepository.findAll(specification);
        return Response.ok(contractMapper.toListComboboxResponse(contracts));
    }

    public Response<com.frezo.qlns.dto.response.ContractResponse> edit(String id, ContractEditRequest request) {
        Contract exist = findEntityById(id);
        validateRequest(request);
        contractMapper.updateEntity(request, exist);
        Contract saveContract = contractRepository.save(exist);
        contractVersionService.createVersion(saveContract, "CONTRACT_UPDATE", "Hồ sơ đã được cập nhật thành công");
        return Response.ok(contractMapper.toResponse(saveContract));
    }

    private void validateRequest(ContractAddRequest request) {
        if (contractRepository.existsByCode(request.getCode())) {
            throw new QTHTException("valid.code.exists");
        }
    }

    private Contract findEntityById(String id) {
        return contractRepository.findById(id).orElseThrow(() -> new QTHTException("valid.not.found"));
    }

    public Response<?> delete(String id) {
        Contract exits = findEntityById(id);
        exits.setIsDeleted(true);
        return Response.ok(contractRepository.save(exits));
    }

    public com.frezo.qlns.dto.response.ContractResponse view(String id) {
        Contract contract = findEntityById(id);
        ContractAssginWork assignWork = contractAssginWorkReposirory.findByContractId(id);

        if (assignWork != null) {
            ContractAssginWorkEditRequest checkRequest = new ContractAssginWorkEditRequest();
            checkRequest.setAssignOP(assignWork.getAssignOP());
            checkRequest.setAssignRV(assignWork.getAssignRV());
            checkView(id, checkRequest);
        }

        return contractMapper.toResponse(contract);
    }

    public ContractAsginWorkResponse assginWork(String contractId, ContractAssginWorkAddRequest request) {
        Contract contract = findEntityById(contractId);
        ContractAssginWork contractAssginWork = contractAssiginWorkMapper.toEntity(request);
        ContractAssginWork response = contractAssginWorkReposirory.save(contractAssginWork);

        if (SystemUtils.isNotNullOrEmpty(request.getAssignRV())) {
            notificationService.sendToUser(
                    request.getAssignRV(),
                    WebSocketChannels.Notify.USER_NOTIF,
                    Map.of(
                            "type", "CONTRACT_ASSIGNED",
                            "contractId", contractId,
                            "message", "Bạn có một hồ sơ mới cần xét duyệt: " + contract.getName()));
        }

        return contractAssiginWorkMapper.toResponse(response);
    }

    public ContractAsginWorkResponse getAssignWork(String contractId) {
        ContractAssginWork assignWork = contractAssginWorkReposirory.findByContractId(contractId);
        if (assignWork == null) {
            return null;
        }
        return contractAssiginWorkMapper.toResponse(assignWork);
    }


    public ContractResponse updateStatus(String id, ContractAddRequest request) {
        Contract contract = findEntityById(id);
        String currentUsername = SystemUtils.getCurrentUsername();
        StatusContarct currentStatus = contract.getStatus();
        StatusContarct newStatus = determineNewStatus(currentUsername, request, currentStatus);

        if (newStatus != null) {
            contract.setStatus(newStatus);
            contractRepository.save(contract);
        }

        return contractMapper.toResponse(contract);
    }

    private StatusContarct determineNewStatus(String username, ContractAddRequest request,
            StatusContarct currentStatus) {
        if (username.equals(request.getAssignOP()) && currentStatus == StatusContarct.OP_PROCESSING) {
            return StatusContarct.OP_DONE;
        }
        if (username.equals(request.getAssignRV()) && currentStatus == StatusContarct.RV_REVIEWING) {
            return StatusContarct.RV_DONE;
        }
        return null;
    }

    private void checkView(String contractId, ContractAssginWorkEditRequest request) {
        Contract contract = findEntityById(contractId);
        String currentUsername = SystemUtils.getCurrentUsername();
        StatusContarct currentStatus = contract.getStatus();

        if (currentUsername.equals(request.getAssignOP())) {
            if (currentStatus == StatusContarct.WAITING_FOR_OP || currentStatus == StatusContarct.DRAFT) {
                contract.setStatus(StatusContarct.OP_PROCESSING);
                contractRepository.save(contract);
            }
        } else if (currentUsername.equals(request.getAssignRV())) {
            if (currentStatus == StatusContarct.WAITING_FOR_RV) {
                contract.setStatus(StatusContarct.RV_REVIEWING);
                contractRepository.save(contract);
            }
        }
    }

    public ContractResponse reject(String id) {
        Contract contract = findEntityById(id);
        String currentUsername = SystemUtils.getCurrentUsername();
        ContractAssginWork assignWork = contractAssginWorkReposirory.findByContractId(id);

        if (assignWork == null) {
            throw new QTHTException("contract.not.found");
        }
        if (!currentUsername.equals(assignWork.getAssignRV())) {
            throw new QTHTException("can.not.role");
        }

        contract.setStatus(StatusContarct.RV_REJECTED);
        contractRepository.save(contract);

        if (SystemUtils.isNotNullOrEmpty(assignWork.getAssignOP())) {
            notificationService.sendToUser(
                    assignWork.getAssignOP(),
                    WebSocketChannels.Notify.USER_NOTIF,
                    Map.of(
                            "type", "CONTRACT_REJECTED",
                            "contractId", id,
                            "message", "Hồ sơ của bạn đã bị từ chối/yêu cầu sửa đổi: " + contract.getName()));
        }

        return contractMapper.toResponse(contract);
    }

    @Override
    public void updateAiStatus(String id, String aiStatus) {
        Contract contract = findEntityById(id);
        contract.setAiStatus(aiStatus);
        contractRepository.save(contract);
    }

    @Override
    public void updateHtmlContract(String id, String html) {
        Contract contract = findEntityById(id);
        contract.setHtmlContract(html);
        contractRepository.save(contract);
    }
}