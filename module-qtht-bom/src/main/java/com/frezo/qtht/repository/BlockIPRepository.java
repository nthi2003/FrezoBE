package com.frezo.qtht.repository;

import com.frezo.qtht.entity.BlockIP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlockIPRepository extends JpaRepository<BlockIP, String> {
    @Query("SELECT b FROM BlockIP b WHERE " +
            "b.ipAddress = :ipAddress AND " +
            "b.targetUserName = :targetUserName AND " +
            "b.blockedUntil > :blockedUntil")
    List<BlockIP> findByIpAddressAndTargetUserNameAndBlockedUntilAfter(
            @Param("ipAddress") String ipAddress,
            @Param("targetUserName") String targetUserName,
            @Param("blockedUntil") LocalDateTime blockedUntil);

    Optional<BlockIP> findByIpAddressAndTargetUserName(
            String ipAddress,
            String targetUserName);

    void deleteByIpAddressAndTargetUserName(
            String ipAddress,
            String targetUserName);

    @Query("SELECT SUM(b.failedAttempts) FROM BlockIP b WHERE b.targetUserName = :targetUserName AND b.updatedDate > :since")
    Integer sumFailedAttemptsByTargetUserName(
            @Param("targetUserName") String targetUserName,
            @Param("since") LocalDateTime since);
}