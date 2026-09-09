package com.frezo.qtht.service.impl;

import com.frezo.auth.dto.request.RegisterRequest;
import com.frezo.auth.entity.User;
import com.frezo.auth.entity.UserRole;
import com.frezo.auth.repository.UserRepository;
import com.frezo.auth.repository.UserRoleRepository;
import com.frezo.common.exception.AppException;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qtht.dto.response.UserResponse;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.repository.RoleRepository;
import com.frezo.qtht.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private static final String APP_CODE = "QTHT";
    private static final String DEFAULT_ROLE_CODE = "STAFF";
    private static final String DEFAULT_ROLE_NAME = "Staff";
    private static final String DEFAULT_ROLE_DESC = "Nhân viên";

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Đăng ký người dùng mới — kiểm tra trùng username/email, tạo User và Person (nếu cần), gán vai trò mặc định hoặc theo yêu cầu.
     */
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        User user = createUser(request);
        Person person;

        if (StringUtils.hasText(request.getPersonId())) {
            person = personRepository.findById(request.getPersonId())
                    .orElseThrow(() -> new AppException(QthtErrorCode.PERSON_REGISTER_NOT_FOUND));
            user.setName(person.getName());
            user.setEmail(person.getEmail());
        } else {
            person = createPerson(request);
        }

        user.setPersonId(person.getId());
        userRepository.save(user);

        assignRoleToUser(request, user);
    }

    /**
     * Gán vai trò cho người dùng theo mã vai trò và mã ứng dụng — kiểm tra trùng lặp trước khi lưu.
     */
    @Override
    @Transactional
    public void assignRole(String username, String roleCode, String appCode) {
        User user = getUserByUsername(username);
        Role role = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(roleCode, appCode)
                .orElseThrow(() -> new AppException(QthtErrorCode.ROLE_NOT_FOUND));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new AppException(QthtErrorCode.USER_ROLE_EXISTS);
        }

        saveUserRole(user, role.getId());
    }

    /**
     * Gán vai trò cho người dùng theo userId và roleId — kiểm tra tồn tại và trùng lặp.
     */
    @Override
    @Transactional
    public void assignRoleById(String userId, String roleId) {
        User user = getUserByIdOrThrow(userId);

        if (!roleRepository.existsById(roleId)) {
            throw new AppException(QthtErrorCode.ROLE_NOT_FOUND);
        }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new AppException(QthtErrorCode.USER_ROLE_EXISTS);
        }

        saveUserRole(user, roleId);
    }

    /**
     * Lấy danh mã vai trò (roleCode) của người dùng theo username.
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getRoles(String username) {
        User user = getUserByUsername(username);
        List<String> roleIds = getRoleIdsByUserId(user.getId());
        return roleRepository.findAllById(roleIds).stream()
                .map(Role::getCode)
                .toList();
    }

    /**
     * Lấy danh sách entity Role của người dùng theo userId.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Role> getRolesByUserId(String userId) {
        List<String> roleIds = getRoleIdsByUserId(userId);
        return roleRepository.findAllById(roleIds);
    }

    /**
     * Lấy danh sách người dùng phân trang —join Person và Role để hiển thị đầy đủ thông tin.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Integer pageNumber, Integer pageSize, String search) {
        Pageable pageable = ServiceHelper.createPageable(
                pageNumber != null ? pageNumber : 0,
                pageSize != null ? pageSize : 20,
                Sort.by(Sort.Direction.DESC, "createdDate"));

        Specification<User> spec = createUserSpecification(search);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<User> users = userPage.getContent();
        Map<String, Person> personMap = fetchPersonsForUsers(users);
        Map<String, List<Role>> userRolesMap = fetchRolesForUsers(users);

        return PageResponse.from(userPage, user ->
                mapToUserResponse(user, personMap.get(user.getPersonId()), userRolesMap.get(user.getId())));
    }

    /**
     * Lấy thông tin chi tiết người dùng theo id — bao gồm Person liên kết và danh sách vai trò.
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = getUserByIdOrThrow(id);

        Person person = null;
        if (user.getPersonId() != null) {
            person = personRepository.findById(user.getPersonId()).orElse(null);
        }

        List<Role> roles = getRolesByUserId(user.getId());
        return mapToUserResponse(user, person, roles);
    }

    /**
     * Kiểm tra dữ liệu đăng ký — dataAction hợp lệ, username chưa tồn tại, email/chưa bắt buộc nếu chưa có personId.
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getDataAction() == null ||
                (request.getDataAction() != 1 && request.getDataAction() != 2 && request.getDataAction() != 3)) {
            throw new AppException(QthtErrorCode.DATA_ACTION_INVALID, request.getDataAction());
        }

        if (userRepository.findByUserName(request.getUsername()).isPresent()) {
            throw new AppException(QthtErrorCode.USER_EXISTS);
        }

        if (!StringUtils.hasText(request.getPersonId())) {
            if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getFullname())) {
                throw new AppException(QthtErrorCode.EMAIL_NAME_REQUIRED);
            }
            if (personRepository.existsByEmail(request.getEmail())) {
                throw new AppException(QthtErrorCode.EMAIL_EXISTS);
            }
        }
    }

    /**
     * Tạo mới entity User từ yêu cầu đăng ký, mã hóa mật khẩu và lưu vào CSDL.
     */
    private User createUser(RegisterRequest request) {
        User user = User.builder()
                .userName(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .name(request.getFullname())
                .dataAction(request.getDataAction())
                .status(1)
                .build();
        user.setIsDeleted(false);
        user.setId(randomUUID().toString());
        return userRepository.save(user);
    }

    /**
     * Tạo mới entity Person từ yêu cầu đăng ký (khi không có personId), thiết lập tổ chức và phòng ban nếu có.
     */
    private Person createPerson(RegisterRequest request) {
        Person person = Person.builder()
                .name(request.getFullname())
                .email(request.getEmail())
                .activated(true)
                .isAdmin(false)
                .build();
        person.setIsDeleted(false);
        person.setId(randomUUID().toString());

        if (StringUtils.hasText(request.getOrgId())) {
            Organization org = new Organization();
            org.setId(request.getOrgId());
            person.setOrganization(org);
        }

        if (StringUtils.hasText(request.getDepartmentId())) {
            Department dept = new Department();
            dept.setId(request.getDepartmentId());
            person.setDepartment(dept);
        }

        return personRepository.save(person);
    }

    /**
     * Gán vai trò cho người dùng từ danh sách roleIds hoặc roleId đơn, nếu không có thì gán vai trò mặc định STAFF.
     */
    private void assignRoleToUser(RegisterRequest request, User user) {
        List<String> roleCodes = request.getRoleIds();
        if (roleCodes != null && !roleCodes.isEmpty()) {
            for (String code : roleCodes) {
                Role role = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(code, APP_CODE)
                        .orElseThrow(
                                () -> new AppException(QthtErrorCode.ROLE_NOT_FOUND));
                saveUserRole(user, role.getId());
            }
        } else if (StringUtils.hasText(request.getRoleId())) {
            Role role = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(request.getRoleId(), APP_CODE)
                    .orElseThrow(
                            () -> new AppException(QthtErrorCode.ROLE_NOT_FOUND));
            saveUserRole(user, role.getId());
        } else {
            assignDefaultRole(user);
        }
    }

    /**
     * Gán vai trò mặc định STAFF cho người dùng — tìm hoặc tạo vai trò mặc định nếu chưa có.
     */
    private void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(DEFAULT_ROLE_CODE, APP_CODE)
                .orElseGet(this::createDefaultRole);
        saveUserRole(user, defaultRole.getId());
    }

    /**
     * Tạo vai trò mặc định STAFF trong CSDL khi chưa tồn tại.
     */
    private Role createDefaultRole() {
        Role newRole = Role.builder()
                .code(DEFAULT_ROLE_CODE)
                .appCode(APP_CODE)
                .name(DEFAULT_ROLE_NAME)
                .description(DEFAULT_ROLE_DESC)
                .build();
        newRole.setIsDeleted(false);
        return roleRepository.save(newRole);
    }
    /**
     * Kích hoạt tài khoản người dùng (đặt status = 1).
     */
    public void actived (String id) {
        User user =  userRepository.findById(id).orElseThrow(() -> new AppException(QthtErrorCode.USER_NOT_FOUND));
        user.setStatus(1);
        userRepository.save(user);
    }
    /**
     * Vô hiệu hóa tài khoản người dùng (đặt status = 0).
     */
    public void inactived (String id) {
        User user =  userRepository.findById(id).orElseThrow(() -> new AppException(QthtErrorCode.USER_NOT_FOUND));
        user.setStatus(0);
        userRepository.save(user);
    }
    
    /**
     * Đặt lại mật khẩu người dùng về "123456" (mã hóa) và trả về mật khẩu mặc định.
     */
    @Override
    @Transactional
    public String resetPassword(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(QthtErrorCode.USER_NOT_FOUND));
        
        String defaultPass = "123456";
        user.setPassword(passwordEncoder.encode(defaultPass));
        userRepository.save(user);
        return defaultPass;
    }


    /**
     * Lưu UserRole — kiểm tra trùng lặp trước khi tạo mới bản ghi UserRole.
     */
    private void saveUserRole(User user, String roleId) {
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), roleId)) {
            return;
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .roleId(roleId)
                .status(1)
                .build();
        userRole.setIsDeleted(false);
        userRole.setId(randomUUID().toString());
        userRoleRepository.save(userRole);
    }

    /**
     * Tìm người dùng theo username, ném ngoại lệ nếu không tìm thấy.
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(QthtErrorCode.USER_NOT_FOUND));
    }

    /**
     * Tìm người dùng theo mã định danh, ném ngoại lệ nếu không tìm thấy.
     */
    private User getUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(QthtErrorCode.USER_NOT_FOUND));
    }

    /**
     * Lấy danh sách roleId của người dùng từ bảng UserRole chưa bị xóa.
     */
    private List<String> getRoleIdsByUserId(String userId) {
        return userRoleRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .map(UserRole::getRoleId)
                .toList();
    }

    /**
     * Xây dựng Specification lọc người dùng — chỉ lấy bản ghi chưa xóa và tìm theo userName, name, email nếu có từ khóa.
     */
    private Specification<User> createUserSpecification(String search) {
        Specification<User> spec = Specification.where((root, query, cb) -> cb.equal(root.get("isDeleted"), false));

        if (StringUtils.hasText(search)) {
            String searchLower = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("userName")), searchLower),
                    cb.like(cb.lower(root.get("name")), searchLower),
                    cb.like(cb.lower(root.get("email")), searchLower)));
        }
        return spec;
    }


    /**
     * Lấy tất cả Person liên kết với danh sách User theo personId (batch query).
     */
    private Map<String, Person> fetchPersonsForUsers(List<User> users) {
        List<String> personIds = users.stream()
                .map(User::getPersonId)
                .filter(Objects::nonNull)
                .toList();

        if (personIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return personRepository.findAllById(personIds).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
    }

    /**
     * Lấy danh sách Role của từng người dùng trong danh sách (dùng để hiển thị trên bảng).
     */
    private Map<String, List<Role>> fetchRolesForUsers(List<User> users) {
        List<String> userIds = users.stream().map(User::getId).toList();

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }


        Map<String, List<Role>> map = new HashMap<>();
        for (User user : users) {
            map.put(user.getId(), getRolesByUserId(user.getId()));
        }
        return map;
    }

    /**
     * Cập nhật thông tin người dùng (username, email, name, personId) và thay thế danh sách vai trò.
     */
    @Override
    @Transactional
    public UserResponse updateUser(String id, Map<String, Object> updates) {
        User user = getUserByIdOrThrow(id);

        if (updates.containsKey("username")) {
            user.setUserName((String) updates.get("username"));
        }
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("fullName") || updates.containsKey("name")) {
            String name = (String) updates.getOrDefault("fullName", updates.get("name"));
            user.setName(name);
        }
        if (updates.containsKey("personId")) {
            String newPersonId = (String) updates.get("personId");
            if (StringUtils.hasText(newPersonId) && !newPersonId.equals(user.getPersonId())) {
                user.setPersonId(newPersonId);
            }
        }

        userRepository.save(user);

        if (updates.containsKey("roleIds")) {
            Object roleIdsObj = updates.get("roleIds");
            List<String> roleCodes = null;
            if (roleIdsObj instanceof List) {
                roleCodes = ((List<?>) roleIdsObj).stream()
                        .map(Object::toString)
                        .toList();
            }

            // Delete existing user roles
            List<UserRole> existingUserRoles = userRoleRepository.findByUserId(user.getId());
            userRoleRepository.deleteAll(existingUserRoles);

            // Save new user roles
            if (roleCodes != null) {
                for (String code : roleCodes) {
                    Optional<Role> roleOpt = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(code, APP_CODE);
                    roleOpt.ifPresent(role -> saveUserRole(user, role.getId()));
                }
            }
        }

        Person person = null;
        if (user.getPersonId() != null) {
            person = personRepository.findById(user.getPersonId()).orElse(null);
        }

        List<Role> roles = getRolesByUserId(user.getId());
        return mapToUserResponse(user, person, roles);
    }

    /**
     * Chuyển đổi User + Person + Roles sang UserResponse DTO để hiển thị trên giao diện.
     */
    private UserResponse mapToUserResponse(User user, Person person, List<Role> roles) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .username(user.getUserName())
                .name(user.getName())
                .email(user.getEmail())
                .dataAction(user.getDataAction())
                .status(user.getStatus())
                .personId(user.getPersonId());

        if (person != null) {
            builder.phone(person.getPhone())
                    .dob(person.getDob())
                    .gender(person.getGender())
                    .address(person.getAddress())
                    .jobTitle(person.getJobTitle())
                    .activated(person.getActivated())
                    .isAdmin(person.getIsAdmin());

            if (person.getOrganization() != null) {
                builder.orgId(person.getOrganization().getId());
                builder.orgName(person.getOrganization().getName());
            }

            if (person.getDepartment() != null) {
                builder.departmentId(person.getDepartment().getId());
                builder.departmentName(person.getDepartment().getName());
            }
        }

        if (roles != null && !roles.isEmpty()) {
            builder.roles(roles.stream().map(Role::getCode).toList());
            builder.roleNames(roles.stream().map(Role::getName).toList());
        }

        return builder.build();
    }
}
