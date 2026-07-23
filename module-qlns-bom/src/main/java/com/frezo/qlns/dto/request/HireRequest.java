package com.frezo.qlns.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Body cho {@code POST .../applications/{id}/hire} và optional khi accept Offer.
 * LNK-06 policy A: bắt buộc username + password + roleCode khi
 * {@code qlns.recruitment.hire.require-user-account=true}.
 */
@Getter
@Setter
public class HireRequest {

    /** Username login (bắt buộc khi policy A). */
    private String username;

    /** Password tạm (bắt buộc khi policy A; min 6 theo RegisterRequest). */
    private String password;

    /** Role code QTHT (vd. STAFF, MANAGER) — bắt buộc khi policy A. */
    private String roleCode;
}
