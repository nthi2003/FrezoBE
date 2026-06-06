package com.frezo.qtht.repository;

import com.frezo.qtht.entity.IpBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, String> {
    
    @Query("SELECT i FROM IpBlacklist i WHERE i.ipAddress = :ip AND (i.active = true OR i.bannedUntil > :now)")
    Optional<IpBlacklist> findActiveBanByIp(@Param("ip") String ip, @Param("now") LocalDateTime now);

    Optional<IpBlacklist> findByIpAddressAndActiveTrue(String ipAddress);
}
