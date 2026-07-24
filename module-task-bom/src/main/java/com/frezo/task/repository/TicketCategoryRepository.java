package com.frezo.task.repository;

import com.frezo.task.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, String> {

    boolean existsByCodeAndIsDeletedFalse(String code);

    Optional<TicketCategory> findByIdAndIsDeletedFalse(String id);

    Optional<TicketCategory> findByCodeAndIsDeletedFalse(String code);

    List<TicketCategory> findByIsDeletedFalseOrderBySortOrderAscNameAsc();

    List<TicketCategory> findByActiveTrueAndIsDeletedFalseOrderBySortOrderAscNameAsc();
}
