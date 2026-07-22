package com.frezo.qtht.repository;

import com.frezo.qtht.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {

    Optional<UserDevice> findByExpoPushToken(String expoPushToken);

    List<UserDevice> findByUsernameAndIsActiveTrue(String username);

    List<UserDevice> findByUsernameInAndIsActiveTrue(List<String> usernames);
}
