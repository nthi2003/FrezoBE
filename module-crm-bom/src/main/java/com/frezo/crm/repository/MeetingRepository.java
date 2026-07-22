package com.frezo.crm.repository;

import com.frezo.crm.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    List<Meeting> findByIsDeletedFalseOrderByStartAtDesc();
    List<Meeting> findByDealIdAndIsDeletedFalse(String dealId);
}
