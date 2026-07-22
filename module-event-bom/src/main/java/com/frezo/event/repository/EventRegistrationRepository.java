package com.frezo.event.repository;

import com.frezo.event.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {

    List<EventRegistration> findByEventIdAndIsDeletedFalseOrderByRegisteredAtDesc(String eventId);

    List<EventRegistration> findByUsernameAndIsDeletedFalseOrderByRegisteredAtDesc(String username);

    Optional<EventRegistration> findByEventIdAndUsernameAndIsDeletedFalse(String eventId, String username);

    long countByEventIdAndRsvpStatusAndIsDeletedFalse(String eventId, String rsvpStatus);
}
