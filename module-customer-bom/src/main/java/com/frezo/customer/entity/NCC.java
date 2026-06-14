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
@Table(name = "nccs")
@EntityListeners(PhoneEncryptionListener.class)
public class NCC extends BaseEntity implements PhoneEncryptable {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "representative", length = 255)
    private String representative;

    @Column(name = "phone", length = 20)
    private String phone; // Plain text phone is now ephemeral or we can keep it but not save to DB

    @Column(name = "phone_encrypted", columnDefinition = "bytea")
    private byte[] phoneEncrypted;

    @Column(name = "phone_hash", length = 64)
    private String phoneHash;

    @Column(name = "phone_last4", length = 4)
    private String phoneLast4;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "classification_code", length = 50)
    private String classificationCode; // groupCode = "PhanLoaiNCC"

    @Column(name = "growing_area")
    private Double growingArea; // Diện tích vùng trồng

    @Column(name = "max_capacity")
    private Double maxCapacity; // Năng lực cung ứng tối đa/ngày

    @Column(name = "strengths", length = 1000)
    private String strengths; // Danh mục sản phẩm thế mạnh
}
