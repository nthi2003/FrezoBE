package com.frezo.auth.repository;

import com.frezo.auth.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, String> {
    List<LoginHistory> findByUserNameOrderByLoginTimeDesc(String userName);

    List<LoginHistory> findTop3ByUserNameOrderByLoginTimeDesc(String userName);
}
