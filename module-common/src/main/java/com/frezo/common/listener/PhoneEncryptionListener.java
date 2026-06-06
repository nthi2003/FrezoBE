package com.frezo.common.listener;

import com.frezo.common.domain.PhoneEncryptable;
import com.frezo.common.utils.CryptoUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class PhoneEncryptionListener {

    @PrePersist
    @PreUpdate
    public void encryptPhone(Object entity) {
        if (entity instanceof PhoneEncryptable encryptable) {
            String phone = encryptable.getPhone();
            if (phone != null && !phone.isEmpty()) {
                encryptable.setPhoneEncrypted(CryptoUtils.encryptAESGCM(phone));
                encryptable.setPhoneHash(CryptoUtils.hashSHA256(phone));
                encryptable.setPhoneLast4(CryptoUtils.getLast4Digits(phone));
                encryptable.setPhone(null); // Clear raw phone before saving to DB
            }
        }
    }
}
