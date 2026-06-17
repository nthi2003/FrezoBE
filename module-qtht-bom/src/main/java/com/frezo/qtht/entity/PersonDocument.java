package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "person_document")
public class PersonDocument extends BaseEntity {

    @Column(name = "person_id", nullable = false, length = 36)
    private String personId;

    @Column(name = "type", nullable = false, length = 30)
    private String type; // CV, CERTIFICATE, ACHIEVEMENT

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;
}
