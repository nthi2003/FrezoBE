package com.frezo.common.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "code_sequences")
public class CodeSequence {

    @Id
    @Column(name = "prefix", length = 20, nullable = false)
    private String prefix;

    @Column(name = "current_seq", nullable = false)
    private Long currentSeq;

    @Column(name = "description", length = 255)
    private String description;
}
