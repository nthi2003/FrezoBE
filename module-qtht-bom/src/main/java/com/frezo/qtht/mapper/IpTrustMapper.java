package com.frezo.qtht.mapper;
import com.frezo.qtht.dto.request.IpTrustAddRequest;
import com.frezo.qtht.dto.request.IpTrustEditRequest;
import com.frezo.qtht.dto.response.IpTrustResponse;
import com.frezo.qtht.entity.IPTrust;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IpTrustMapper {

    IpTrustResponse toResponse(IPTrust ipTrust);

    List<IpTrustResponse> toResponseList(List<IPTrust> ipTrusts);

    IPTrust toEntity(IpTrustAddRequest request);



    void updateEntity(IpTrustEditRequest request, @MappingTarget IPTrust ipTrust);
}
