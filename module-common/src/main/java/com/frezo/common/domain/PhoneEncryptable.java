package com.frezo.common.domain;

public interface PhoneEncryptable {
    String getPhone();
    void setPhone(String phone);
    void setPhoneEncrypted(byte[] phoneEncrypted);
    void setPhoneHash(String phoneHash);
    void setPhoneLast4(String phoneLast4);
}
