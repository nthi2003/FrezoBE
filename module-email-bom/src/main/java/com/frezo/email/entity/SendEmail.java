package com.frezo.email.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "send_emails")
public class SendEmail extends BaseEntity {

    private String emailTemplateId;
    private String topic;

    @ElementCollection
    @CollectionTable(name = "send_email_recipients", joinColumns = @JoinColumn(name = "send_email_id"))
    private List<String> recipients;

    private String description;

    @ElementCollection
    @CollectionTable(name = "send_email_files", joinColumns = @JoinColumn(name = "send_email_id"))
    private List<String> file;
}
