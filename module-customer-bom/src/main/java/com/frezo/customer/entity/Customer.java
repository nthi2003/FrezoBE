package com.frezo.customer.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.common.domain.PhoneEncryptable;
import com.frezo.common.listener.PhoneEncryptionListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
@EntityListeners(PhoneEncryptionListener.class)
public class Customer extends BaseEntity implements PhoneEncryptable {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "phone_encrypted", columnDefinition = "bytea")
    private byte[] phoneEncrypted;

    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    @Column(name = "phone_last4", length = 4)
    private String phoneLast4;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "type", length = 50)
    private String type; // e.g. INDIVIDUAL, COMPANY

    @Column(name = "status", length = 50)
    private String status; // e.g. ACTIVE, INACTIVE

    @Column(name = "category_code", length = 50)
    private String categoryCode; // Ref to Category with group_code KHTN

    @Column(name = "note", length = 1000)
    private String note;
}
