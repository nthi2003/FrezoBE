package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.qtht.dto.request.OrganizationAddRequest;
import com.frezo.qtht.dto.request.OrganizationEditRequest;
import com.frezo.qtht.dto.request.OrganizationFilterRequest;
import com.frezo.qtht.dto.response.OrganizationDetailResponse;
import com.frezo.qtht.dto.response.OrganizationResponse;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.mapper.OrganizationMapper;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.OrganizationService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final PersonRepository personRepository;
    private final com.frezo.qtht.service.SettingService settingService;

    /**
     * Lấy danh sách tổ chức phân trang theo bộ lọc (từ khóa tìm kiếm, parentId, ...).
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrganizationResponse> all(OrganizationFilterRequest filter) {
        Specification<Organization> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Organization> entities = organizationRepository.findAll(
                specification, ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort, true));

        return PageResponse.from(entities, organizationMapper::toResponse);
    }

    /**
     * Thêm mới tổ chức — kiểm tra trùng mã/mã số thuế, xử lý tổ chức cha, đại diện pháp luật và tạo setting mặc định.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<OrganizationResponse> add(OrganizationAddRequest request) {
        validateRequest(request);

        Organization organization = organizationMapper.toEntity(request);
        organization.setIsDeleted(false);

        processParentAndLevel(request.getParentId(), request.getLevel(), organization);
        updateLegalRepresentative(organization, request.getLegalRepresentativeId());

        Organization savedOrg = organizationRepository.save(organization);

        // Create default setting for the new organization
        com.frezo.qtht.dto.request.SettingAddRequest settingRequest = new com.frezo.qtht.dto.request.SettingAddRequest();
        settingRequest.setOrgId(savedOrg.getId());
        settingRequest.setIsAttendance(false);
        settingRequest.setIsColor(false);
        settingRequest.setIsEmail(false);
        settingRequest.setIsSwap(false);
        settingService.add(settingRequest);

        return ApiResponse.ok(organizationMapper.toResponse(savedOrg));
    }

    /**
     * Cập nhật thông tin tổ chức — kiểm tra trùng lặp, cập nhật tổ chức cha và đại diện pháp luật.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<OrganizationResponse> update(String id, OrganizationEditRequest request) {
        Organization organization = findEntityById(id);

        validateUpdate(organization, request);
        organizationMapper.updateEntity(request, organization);

        boolean parentChanged = isParentChanged(organization, request.getParentId());
        if (parentChanged) {
            processParentAndLevel(request.getParentId(), request.getLevel(), organization);
        }

        updateLegalRepresentative(organization, request.getLegalRepresentativeId());

        log.debug("Updating organization: {}", organization.getCode());
        Organization savedOrg = organizationRepository.save(organization);

        return ApiResponse.ok(organizationMapper.toResponse(savedOrg));
    }

    /**
     * Xóa mềm tổ chức (đánh dấu isDeleted = true).
     */
    @Override
    @Transactional
    public void delete(String id) {
        Organization organization = findEntityById(id);
        organization.setIsDeleted(true);
        organizationRepository.save(organization);
        log.debug("Deleted organization: {}", id);
    }

    /**
     * Lấy chi tiết tổ chức theo mã định danh.
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<OrganizationDetailResponse> getById(String id) {
        Organization organization = findEntityById(id);
        return ApiResponse.ok(organizationMapper.toDetailResponse(organization));
    }

    /**
     * Lấy danh sách tổ chức con theo parentId (hiện tại trả về danh sách rỗng).
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getChildren(String parentId) {
        return Collections.emptyList();
    }

    /**
     * Lấy danh sách tổ chức dưới dạng combobox (value=label=id, label=tên, description=mã).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComboboxResponse> getCombobox(OrganizationFilterRequest filter) {
        PageResponse<OrganizationResponse> data = all(filter);
        List<OrganizationResponse> items = data.getItems() != null ? data.getItems() : Collections.emptyList();
        return items.stream()
                .map(org -> ComboboxResponse.builder()
                        .value(org.getId())
                        .label(org.getName())
                        .description(org.getCode())
                        .build())
                .toList();
    }



    /**
     * Tìm tổ chức theo mã định danh, ném ngoại lệ nếu không tìm thấy hoặc đã bị xóa.
     */
    @Transactional(readOnly = true)
    protected Organization findEntityById(String id) {
        return organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(QthtErrorCode.ORGANIZATION_NOT_FOUND));
    }

    /**
     * Kiểm tra mã tổ chức và mã số thuế có trùng lặp khi thêm mới.
     */
    private void validateRequest(OrganizationAddRequest request) {
        if (organizationRepository.existsByCode(request.getCode())) {
            throw new AppException(QthtErrorCode.ORGANIZATION_CODE_EXISTS, request.getCode());
        }
        if (StringUtils.hasText(request.getTaxCode()) && organizationRepository.existsByTaxCode(request.getTaxCode())) {
            throw new AppException(QthtErrorCode.ORGANIZATION_TAX_EXISTS, request.getTaxCode());
        }
    }

    /**
     * Kiểm tra mã tổ chức và mã số thuế có trùng lặp khi cập nhật (bỏ qua bản ghi hiện tại).
     */
    private void validateUpdate(Organization organization, OrganizationEditRequest request) {
        if (!organization.getCode().equals(request.getCode())
                && organizationRepository.existsByCode(request.getCode())) {
            throw new AppException(QthtErrorCode.ORGANIZATION_CODE_EXISTS, request.getCode());
        }

        if (StringUtils.hasText(request.getTaxCode()) && !request.getTaxCode().equals(organization.getTaxCode())
                && organizationRepository.existsByTaxCode(request.getTaxCode())) {
            throw new AppException(QthtErrorCode.ORGANIZATION_TAX_EXISTS, request.getTaxCode());
        }
    }

    /**
     * Xử lý tổ chức cha, kiểm tra cấp độ hợp lệ và cập nhật đường dẫn (path) phân cấp.
     */
    private void processParentAndLevel(String parentId, Integer level, Organization organization) {
        if (!StringUtils.hasText(organization.getId())) {
            organization.setId(UUID.randomUUID().toString());
        }

        if (StringUtils.hasText(parentId)) {
            Organization parent = organizationRepository.findById(parentId)
                    .orElseThrow(() -> new AppException(QthtErrorCode.ORGANIZATION_PARENT_NOT_FOUND));

            if (level != null && level <= (parent.getLevel() != null ? parent.getLevel() : 0)) {
                throw new AppException(QthtErrorCode.ORGANIZATION_LEVEL_INVALID);
            }

            organization.setParent(parent);
            organization.setParentId(parent.getId());
            String parentPath = StringUtils.hasText(parent.getPath()) ? parent.getPath() : "/";
            organization.setPath(parentPath + organization.getId() + "/");
        } else {
            organization.setParent(null);
            organization.setParentId(null);
            organization.setPath("/" + organization.getId() + "/");
        }
    }

    /**
     * Cập nhật đại diện pháp luật cho tổ chức (tra cứu Person theo ID).
     */
    private void updateLegalRepresentative(Organization organization, String legalRepId) {
        if (StringUtils.hasText(legalRepId)) {
            Person legalRep = personRepository.findById(legalRepId)
                    .orElseThrow(() -> new AppException(QthtErrorCode.PERSON_NOT_FOUND));
            organization.setLegalRepresentative(legalRep);
        } else {
            organization.setLegalRepresentative(null);
        }
    }

    /**
     * Kiểm tra tổ chức cha có thay đổi so với giá trị hiện tại hay không.
     */
    private boolean isParentChanged(Organization organization, String newParentId) {
        return (newParentId == null && organization.getParentId() != null)
                || (newParentId != null && !newParentId.equals(organization.getParentId()));
    }

    /**
     * Xây dựng Specification lọc tổ chức theo từ khóa (tên, mã, tên viết tắt) và trạng thái chưa xóa.
     */
    private Specification<Organization> createSpecification(OrganizationFilterRequest filter) {
        Specification<Organization> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Organization>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("code", filter.getKeyword()))
                            .or(GenericSpecification.likeField("shortName", filter.getKeyword())));
        }
        return specification;
    }
}
