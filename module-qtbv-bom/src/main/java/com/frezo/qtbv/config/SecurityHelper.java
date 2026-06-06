package com.frezo.qtbv.config;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.entity.User;
import com.frezo.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail)) {
            throw new AppException("Unauthenticated", HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();
        User user = userDetail.getUser();

        // Lấy personId từ User entity
        if (user.getPersonId() == null) {
            throw new AppException("User does not have Person ID", HttpStatus.BAD_REQUEST);
        }

        return user.getPersonId();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail)) {
            throw new AppException("Unauthenticated", HttpStatus.UNAUTHORIZED);
        }

        CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();
        return userDetail.getUser();
    }


}
