package com.frezo.event.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "evt_event", indexes = {
        @Index(name = "idx_evt_status", columnList = "status"),
        @Index(name = "idx_evt_start", columnList = "start_at")
})
public class Event extends BaseEntity {

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** DRAFT / PUBLISHED / CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    /** null = unlimited */
    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "registered_count")
    private Integer registeredCount;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "organizer_username", length = 100)
    private String organizerUsername;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
