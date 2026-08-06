package com.frezo.qlns.entity;

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

/**
 * Vị trí công việc — tham chiếu hạng mục {@code CapBac} và {@code ChucDanh}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hr_job_position", indexes = {
        @Index(name = "idx_hr_job_pos_rank", columnList = "rank_code"),
        @Index(name = "idx_hr_job_pos_title", columnList = "title_code")
})
public class JobPosition extends BaseEntity {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /** Category code — group {@code CapBac}. */
    @Column(name = "rank_code", length = 50, nullable = false)
    private String rankCode;

    /** Category code — group {@code ChucDanh}. */
    @Column(name = "title_code", length = 50, nullable = false)
    private String titleCode;

    @Column(name = "activated", nullable = false)
    private Boolean activated = true;

    @Column(name = "order_index")
    private Integer orderIndex;
}
