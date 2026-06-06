package com.frezo.task.repository;

import com.frezo.task.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    Optional<Ticket> findByCode(String code);
    Page<Ticket> findByStatus(Ticket.TicketStatus status, Pageable pageable);
    Page<Ticket> findByAssigneeId(String assigneeId, Pageable pageable);
    Page<Ticket> findByReporterId(String reporterId, Pageable pageable);
}
