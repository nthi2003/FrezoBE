package com.frezo.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

/**
 * Sequence counter for auto-generated business codes.
 * <p>
 * Implements {@link Persistable} because the PK ({@code prefix}) is assigned
 * by the app before insert — without this, Spring Data treats every save as
 * {@code merge()} (isNew=false when id != null), which breaks first-insert + lock.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "code_sequences")
public class CodeSequence implements Persistable<String> {

    @Id
    @Column(name = "prefix", length = 32, nullable = false)
    private String prefix;

    @Column(name = "current_seq", nullable = false)
    private Long currentSeq;

    @Column(name = "description", length = 255)
    private String description;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return prefix;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
