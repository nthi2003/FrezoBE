package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Department;
import com.frezo.qtht.entity.IPTrust;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IpTrustRepository  extends JpaRepository<IPTrust, String > , JpaSpecificationExecutor<IPTrust> {
    boolean existsByIpName (String ipName);
    Boolean existsByIpNumber (String ipNumber);
}
