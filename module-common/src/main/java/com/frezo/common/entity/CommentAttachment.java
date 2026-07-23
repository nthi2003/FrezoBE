package com.frezo.common.entity;

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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_comment_attachment", indexes = {
        @Index(name = "idx_cmt_att_comment", columnList = "comment_id")
})
public class CommentAttachment extends BaseEntity {

    @Column(name = "comment_id", length = 36, nullable = false)
    private String commentId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_type", length = 120)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    /** Relative MinIO object key for cleanup (optional). */
    @Column(name = "object_name", length = 500)
    private String objectName;
}
