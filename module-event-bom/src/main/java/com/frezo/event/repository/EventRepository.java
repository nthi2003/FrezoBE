package com.frezo.event.repository;

import com.frezo.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findByIsDeletedFalseOrderByStartAtDesc();

    List<Event> findByStatusAndIsDeletedFalseOrderByStartAtAsc(String status);

    List<Event> findByStartAtBetweenAndIsDeletedFalseOrderByStartAtAsc(
            LocalDateTime from, LocalDateTime to);

    List<Event> findByStatusAndStartAtBetweenAndIsDeletedFalseOrderByStartAtAsc(
            String status, LocalDateTime from, LocalDateTime to);
}
