package com.frezo.crm.entity;

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
@Table(name = "crm_meeting", indexes = {
        @Index(name = "idx_meeting_deal", columnList = "deal_id"),
        @Index(name = "idx_meeting_start", columnList = "start_at")
})
public class Meeting extends BaseEntity {

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "deal_id", length = 36)
    private String dealId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    /** JSON array of usernames / emails. */
    @Column(name = "attendees", length = 2000)
    private String attendees;

    /** SCHEDULED / DONE / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "notes", length = 2000)
    private String notes;
}
