package com.frezo.qlns.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.common.constant.WebSocketChannels;
import com.frezo.common.service.NotificationService;
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
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
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

    /**
     * Truy vấn danh sách hợp đồng đang hoạt động theo bộ lọc (từ khoá, hiệu lực, người, trạng thái), phân trang.
     */
    public PageResponse<ContractResponse> all(ContractFilter filter) {
        Specification<Contract> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Contract> entities = contractRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        return PageResponse.from(entities, contractMapper::toResponse);
    }

    /**
     * Dựng Specification (điều kiện lọc) cho truy vấn hợp đồng từ bộ lọc đầu vào.
     */
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
        if (SystemUtils.isNotNullOrEmpty(filter.getPersonId())) {
            specification = specification.and(GenericSpecification.equalField("personId", filter.getPersonId()));
        }
        if (filter.getStatus() != null) {
            specification = specification.and(GenericSpecification.equalField("status", filter.getStatus()));
        }
        return specification;
    }

    /**
     * Thêm mới một hợp đồng, tạo bản ghi lịch sử và version đầu tiên cho hợp đồng.
     */
    public ApiResponse<com.frezo.qlns.dto.response.ContractResponse> add(ContractAddRequest request) {
        validateRequest(request);
        Contract contract = contractMapper.toEntity(request);
        normalizeSalary3P(contract);
        contract.setIsDeleted(false);
        Contract savedContract = contractRepository.save(contract);

        ContractHistory contractHistory = new ContractHistory();
        contractHistory.setContractId(savedContract.getId());
        contractHistoryRepository.save(contractHistory);
        contractVersionService.createVersion(savedContract, "CONTRACT_DONE", "Hồ sơ đã được tạo thành công");

        return ApiResponse.ok(contractMapper.toResponse(savedContract));
    }

    /**
     * Lấy danh sách hợp đồng dạng combobox (mặc định chỉ các hợp đồng đã kích hoạt).
     */
    public ApiResponse<List<com.frezo.qlns.dto.response.ContractComboboxResponse>> combobox(ContractFilter filter) {
        Specification<Contract> specification = createSpecification(filter);
        if (!Boolean.TRUE.equals(filter.getIsDelete())) {
            specification = specification.and(GenericSpecification.hasFieldIs("activated", Boolean.TRUE));
        }
        List<Contract> contracts = contractRepository.findAll(specification);
        return ApiResponse.ok(contractMapper.toListComboboxResponse(contracts));
    }

    /**
     * Cập nhật thông tin hợp đồng hiện có và lưu version mới cho hợp đồng.
     */
    public ApiResponse<com.frezo.qlns.dto.response.ContractResponse> edit(String id, ContractEditRequest request) {
        Contract exist = findEntityById(id);
        validateRequest(request);
        contractMapper.updateEntity(request, exist);
        normalizeSalary3P(exist);
        Contract saveContract = contractRepository.save(exist);
        contractVersionService.createVersion(saveContract, "CONTRACT_UPDATE", "Hồ sơ đã được cập nhật thành công");
        return ApiResponse.ok(contractMapper.toResponse(saveContract));
    }

    /**
     * Kiểm tra mã hợp đồng đã tồn tại hay chưa trước khi thêm mới.
     */
    private void validateRequest(ContractAddRequest request) {
        if (contractRepository.existsByCode(request.getCode())) {
            throw new AppException(QlnsErrorCode.CODE_EXISTS);
        }
    }

    /**
     * Tra cứu hợp đồng theo id, không tồn tại thì ném AppException ENTITY_NOT_FOUND.
     */
    private Contract findEntityById(String id) {
        return contractRepository.findById(id).orElseThrow(() -> new AppException(QlnsErrorCode.ENTITY_NOT_FOUND));
    }

    /**
     * Xoá mềm (soft-delete) một hợp đồng theo id.
     */
    public ApiResponse<?> delete(String id) {
        Contract exits = findEntityById(id);
        exits.setIsDeleted(true);
        return ApiResponse.ok(contractRepository.save(exits));
    }

    /**
     * Xem chi tiết hợp đồng; nếu có giao việc duyệt (assign work) thì chuyển trạng thái khi người phụ trách mở xem.
     */
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

    /**
     * Giao việc duyệt hợp đồng cho người phụ trách (RV) và gửi thông báo WebSocket cho họ.
     */
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

    /**
     * Lấy thông tin giao việc duyệt của một hợp đồng (trả null nếu chưa có).
     */
    public ContractAsginWorkResponse getAssignWork(String contractId) {
        ContractAssginWork assignWork = contractAssginWorkReposirory.findByContractId(contractId);
        if (assignWork == null) {
            return null;
        }
        return contractAssiginWorkMapper.toResponse(assignWork);
    }


    /**
     * Cập nhật trạng thái xử lý hợp đồng (OP/RV) dựa trên người dùng đang đăng nhập.
     */
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

    /**
     * Xác định trạng thái mới: OP hoàn thành khi OP duyệt, RV hoàn thành khi RV duyệt; ngược lại giữ nguyên.
     */
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

    /**
     * Khi người phụ trách mở xem hợp đồng, chuyển trạng thái từ chờ xử lý sang đang xử lý (OP/RV).
     */
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

    /**
     * Người duyệt RV từ chối/ yêu cầu sửa hợp đồng: cập nhật trạng thái RV_REJECTED và thông báo cho OP.
     */
    public ContractResponse reject(String id) {
        Contract contract = findEntityById(id);
        String currentUsername = SystemUtils.getCurrentUsername();
        ContractAssginWork assignWork = contractAssginWorkReposirory.findByContractId(id);

        if (assignWork == null) {
            throw new AppException(QlnsErrorCode.CONTRACT_NOT_FOUND);
        }
        if (!currentUsername.equals(assignWork.getAssignRV())) {
            throw new AppException(QlnsErrorCode.CANNOT_ROLE);
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

    /**
     * Cập nhật trạng thái AI đánh giá hợp đồng.
     */
    @Override
    public void updateAiStatus(String id, String aiStatus) {
        Contract contract = findEntityById(id);
        contract.setAiStatus(aiStatus);
        contractRepository.save(contract);
    }

    /**
     * Cập nhật nội dung HTML của hợp đồng.
     */
    @Override
    public void updateHtmlContract(String id, String html) {
        Contract contract = findEntityById(id);
        contract.setHtmlContract(html);
        contractRepository.save(contract);
    }

    /**
     * Chuẩn hóa lương 3P: value (lương cơ bản payroll) = P1 + P2.
     * FE có thể gửi basicSalary → positionSalary, kpiAmount → kpiBonus.
     */
    private void normalizeSalary3P(Contract contract) {
        if (contract.getPositionSalary() == null && contract.getValue() != null) {
            contract.setPositionSalary(contract.getValue());
        }
        if (contract.getPersonAllowance() == null) {
            contract.setPersonAllowance(0);
        }
        if (contract.getKpiBonus() == null) {
            contract.setKpiBonus(0);
        }
        int p1 = contract.getPositionSalary() != null ? contract.getPositionSalary() : 0;
        int p2 = contract.getPersonAllowance() != null ? contract.getPersonAllowance() : 0;
        contract.setValue(p1 + p2);
    }
}