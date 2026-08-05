package com.frezo.qtht.repository;

import com.frezo.qtht.entity.SystemJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemJobRepository extends JpaRepository<SystemJob, String> {

    Optional<SystemJob> findByJobCodeAndIsDeletedFalse(String jobCode);

    List<SystemJob> findByIsDeletedFalseOrderByModuleCodeAscJobNameAsc();
}
