package com.frezo.qlns.repository;

import com.frezo.qlns.entity.PersonWorkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonWorkHistoryRepository extends JpaRepository<PersonWorkHistory, String> {
    List<PersonWorkHistory> findByPersonIdAndIsDeletedFalseOrderByFromDateDesc(String personId);
}
