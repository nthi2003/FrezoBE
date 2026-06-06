package com.frezo.qtht.service;

import com.frezo.auth.dto.request.RegisterRequest;
import com.frezo.qtht.dto.response.UserResponse;
import com.frezo.qtht.entity.Role;

import java.util.List;
import java.util.Map;

public interface UserAdminService {
    void register(RegisterRequest request);

    void assignRole(String username, String roleCode, String appCode);

    void assignRoleById(String userId, String roleId);

    List<String> getRoles(String username);

    List<Role> getRolesByUserId(String userId);

    Map<String, Object> getAllUsers(Integer pageNumber, Integer pageSize, String search);

    UserResponse getUserById(String id);

    void actived (String id);

    void inactived (String id);
    String resetPassword(String id);
}
