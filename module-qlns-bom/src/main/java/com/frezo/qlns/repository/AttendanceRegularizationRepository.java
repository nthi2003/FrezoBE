package com.frezo.qlns.repository;

import com.frezo.qlns.entity.AttendanceRegularization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRegularizationRepository
        extends JpaRepository<AttendanceRegularization, String>,
                JpaSpecificationExecutor<AttendanceRegularization> {

    List<AttendanceRegularization> findByPersonIdOrderByCreatedDateDesc(String personId);

    List<AttendanceRegularization> findByManagerUsernameAndStatusOrderByCreatedDateDesc(
            String managerUsername, String status);

    List<AttendanceRegularization> findByStatusOrderByCreatedDateDesc(String status);
}
