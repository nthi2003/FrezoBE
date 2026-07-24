package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.dto.request.IpTrustAddRequest;
import com.frezo.qtht.dto.request.IpTrustEditRequest;
import com.frezo.qtht.dto.request.IpTrustFilter;
import com.frezo.qtht.dto.request.MenuPermissionSaveRequest;
import com.frezo.qtht.dto.response.IpTrustResponse;
import com.frezo.qtht.entity.IPTrust;
import com.frezo.qtht.mapper.IpTrustMapper;
import com.frezo.qtht.repository.IpTrustRepository;
import com.frezo.qtht.service.IPTrustService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IPTrustServiceImpl implements IPTrustService {
    private final IpTrustRepository ipTrustRepository;
    private final IpTrustMapper ipTrustMapper;


    public PageResponse<IpTrustResponse> all(IpTrustFilter filter) {
        Specification<IPTrust> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC , "createdDate");
        Page<IPTrust> entities = ipTrustRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(),sort));
        return PageResponse.from(entities, ipTrustMapper::toResponse);
    }

    private Specification<IPTrust> createSpecification (IpTrustFilter filter) {
        Specification<IPTrust> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted" , Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<IPTrust>likeField("ipNumber", filter.getKeyword())
                            .or(GenericSpecification.likeField("ipName", filter.getKeyword())));

        }
        return specification;

    }

    public ApiResponse<IpTrustResponse> add(IpTrustAddRequest request) {

        validateRequest(request);
        IPTrust ipTrust = ipTrustMapper.toEntity(request);
        IPTrust saveipTrust = ipTrustRepository.save(ipTrust);
        return ApiResponse.ok(ipTrustMapper.toResponse(saveipTrust));

    }

    private void validateRequest(IpTrustAddRequest request) {
        if (ipTrustRepository.existsByIpName(request.getIpName())) {
            throw new AppException(QthtErrorCode.IP_NAME_ALREADY_USED);
        }
        if (ipTrustRepository.existsByIpNumber(request.getIpName())) {
            throw new AppException(QthtErrorCode.IP_NUMBER_ALREADY_USED);
        }

    }
    public ApiResponse<IpTrustResponse> edit(String id, IpTrustEditRequest request) {
        IPTrust exist = findEntityById(id);
        validateRequest(request);
        ipTrustMapper.updateEntity(request, exist);
        IPTrust saveipTrust = ipTrustRepository.save(exist);
        return ApiResponse.ok(ipTrustMapper.toResponse(saveipTrust));
    }

    public IpTrustResponse view (String id) {
        IPTrust ipTrust = findEntityById(id);
        return  ipTrustMapper.toResponse(ipTrust);
    }

    public ApiResponse<?> delete(String id) {
        IPTrust exits = findEntityById(id);
        exits.setIsDeleted(true);
        return ApiResponse.ok(ipTrustRepository.save(exits));
    }
    private IPTrust findEntityById(String id) {
        return ipTrustRepository.findById(id).orElseThrow(() -> new AppException(QthtErrorCode.ENTITY_NOT_FOUND));
    }


}
