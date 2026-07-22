package com.frezo.event.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "evt_registration",
        uniqueConstraints = @UniqueConstraint(name = "uk_evt_reg_event_user",
                columnNames = {"event_id", "username"}),
        indexes = {
                @Index(name = "idx_evt_reg_event", columnList = "event_id"),
                @Index(name = "idx_evt_reg_user", columnList = "username")
        })
public class EventRegistration extends BaseEntity {

    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    /** GOING / MAYBE / DECLINED / CANCELLED */
    @Column(name = "rsvp_status", length = 30, nullable = false)
    private String rsvpStatus;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
}
