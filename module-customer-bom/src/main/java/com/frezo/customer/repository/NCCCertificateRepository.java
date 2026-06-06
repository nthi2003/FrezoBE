package com.frezo.customer.repository;

import com.frezo.customer.entity.NCCCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NCCCertificateRepository extends JpaRepository<NCCCertificate, String> {
    List<NCCCertificate> findByNccId(String nccId);
    void deleteByNccId(String nccId);
}
