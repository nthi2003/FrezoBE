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

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "token_wallet", indexes = {
        @Index(name = "uk_token_wallet_person", columnList = "person_id", unique = true)
})
public class TokenWallet extends BaseEntity {

    @Column(name = "person_id", length = 36, nullable = false, unique = true)
    private String personId;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;
}
