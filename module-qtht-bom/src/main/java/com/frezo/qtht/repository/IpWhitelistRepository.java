package com.frezo.qtht.repository;

import com.frezo.qtht.entity.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpWhitelistRepository extends JpaRepository<IpWhitelist, String> {

    Optional<IpWhitelist> findByIpAddress(String ipAddress);

    boolean existsByIpAddress(String ipAddress);

    List<IpWhitelist> findByIsActiveTrue();
}

