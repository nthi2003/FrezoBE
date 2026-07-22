package com.frezo.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "type")
    private String type;

    /** URL FE navigate khi user click notification. */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /** Alias FE — cùng giá trị {@link #actionUrl}. */
    @JsonProperty("link")
    public String getLink() {
        return actionUrl;
    }

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "sender_username", length = 100)
    private String senderUsername;

    @Column(name = "priority", length = 20)
    private String priority;
}
