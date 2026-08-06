package com.frezo.email.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

    /** Kênh gửi — EMAIL hiện tại, chừa chỗ cho SMS/ZALO dùng chung bảng log này. */
    @Column(length = 20)
    private String type;

    /** SUCCESS | FAILED — kết quả gửi thực tế tới SMTP. */
    @Column(length = 20)
    private String status;

    /** Lý do thất bại (SMTP host sai, auth fail, timeout…) — null khi SUCCESS. */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @ElementCollection
    @CollectionTable(name = "send_email_files", joinColumns = @JoinColumn(name = "send_email_id"))
    private List<String> file;
}
