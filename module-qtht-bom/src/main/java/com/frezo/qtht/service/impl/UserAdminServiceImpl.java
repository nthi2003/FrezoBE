package com.frezo.qtht.service.impl;

import com.frezo.auth.dto.request.RegisterRequest;
import com.frezo.auth.entity.User;
import com.frezo.auth.entity.UserRole;
import com.frezo.auth.repository.UserRepository;
import com.frezo.auth.repository.UserRoleRepository;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.qtht.dto.response.UserResponse;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.repository.RoleRepository;
import com.frezo.qtht.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
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

    private static final String ERR_USER_EXISTS = "exception.user.exists";
    private static final String ERR_EMAIL_EXISTS = "exception.email.exists";
    private static final String ERR_ROLE_NOT_FOUND = "exception.role.not.found";
    private static final String ERR_USER_NOT_FOUND = "exception.user.not.found";
    private static final String ERR_USER_ROLE_EXISTS = "exception.userRole.exists";

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        User user = createUser(request);
        Person person = createPerson(request);

        user.setPersonId(person.getId());
        userRepository.save(user);

        assignRoleToUser(request, user);
    }

    @Override
    @Transactional
    public void assignRole(String username, String roleCode, String appCode) {
        User user = getUserByUsername(username);
        Role role = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(roleCode, appCode)
                .orElseThrow(() -> new QTHTException(ERR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            throw new QTHTException(ERR_USER_ROLE_EXISTS, HttpStatus.BAD_REQUEST);
        }

        saveUserRole(user, role.getId());
    }

    @Override
    @Transactional
    public void assignRoleById(String userId, String roleId) {
        User user = getUserByIdOrThrow(userId);

        if (!roleRepository.existsById(roleId)) {
            throw new QTHTException(ERR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new QTHTException(ERR_USER_ROLE_EXISTS, HttpStatus.BAD_REQUEST);
        }

        saveUserRole(user, roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoles(String username) {
        User user = getUserByUsername(username);
        List<String> roleIds = getRoleIdsByUserId(user.getId());
        return roleRepository.findAllById(roleIds).stream()
                .map(Role::getCode)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getRolesByUserId(String userId) {
        List<String> roleIds = getRoleIdsByUserId(userId);
        return roleRepository.findAllById(roleIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAllUsers(Integer pageNumber, Integer pageSize, String search) {
        Pageable pageable = ServiceHelper.createPageable(
                pageNumber != null ? pageNumber : 0,
                pageSize != null ? pageSize : 20,
                Sort.by(Sort.Direction.DESC, "createdDate"));

        Specification<User> spec = createUserSpecification(search);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<User> users = userPage.getContent();
        Map<String, Person> personMap = fetchPersonsForUsers(users);
        Map<String, List<Role>> userRolesMap = fetchRolesForUsers(users);

        List<UserResponse> responses = users.stream()
                .map(user -> mapToUserResponse(user, personMap.get(user.getPersonId()), userRolesMap.get(user.getId())))
                .toList();

        return ServiceHelper.createResponse1(
                pageNumber != null ? pageNumber : 0,
                pageSize != null ? pageSize : 20,
                userPage,
                responses);
    }

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

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getDataAction() == null ||
                (request.getDataAction() != 1 && request.getDataAction() != 2 && request.getDataAction() != 3)) {
            throw new QTHTException("exception.dataAction.invalid", HttpStatus.BAD_REQUEST, request.getDataAction());
        }

        if (userRepository.findByUserName(request.getUsername()).isPresent()) {
            throw new QTHTException(ERR_USER_EXISTS, HttpStatus.BAD_REQUEST);
        }

        if (personRepository.existsByEmail(request.getEmail())) {
            throw new QTHTException(ERR_EMAIL_EXISTS, HttpStatus.BAD_REQUEST);
        }
    }

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

        return personRepository.save(person);
    }

    private void assignRoleToUser(RegisterRequest request, User user) {
        if (StringUtils.hasText(request.getRoleId())) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(
                            () -> new QTHTException(ERR_ROLE_NOT_FOUND, HttpStatus.NOT_FOUND));
            saveUserRole(user, role.getId());
        } else {
            assignDefaultRole(user);
        }
    }

    private void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByCodeAndAppCodeAndIsDeletedFalse(DEFAULT_ROLE_CODE, APP_CODE)
                .orElseGet(this::createDefaultRole);
        saveUserRole(user, defaultRole.getId());
    }

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
    public void actived (String id) {
        User user =  userRepository.findById(id).orElseThrow(() -> new QTHTException(ERR_USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setStatus(1);
        userRepository.save(user);
    }
    public void inactived (String id) {
        User user =  userRepository.findById(id).orElseThrow(() -> new QTHTException(ERR_USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setStatus(0);
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public String resetPassword(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new QTHTException(ERR_USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        
        String defaultPass = "123456";
        user.setPassword(passwordEncoder.encode(defaultPass));
        userRepository.save(user);
        return defaultPass;
    }


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

    private User getUserByUsername(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new QTHTException(ERR_USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private User getUserByIdOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new QTHTException(ERR_USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private List<String> getRoleIdsByUserId(String userId) {
        return userRoleRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .map(UserRole::getRoleId)
                .toList();
    }

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
        }

        if (roles != null && !roles.isEmpty()) {
            builder.roles(roles.stream().map(Role::getCode).toList());
            builder.roleNames(roles.stream().map(Role::getName).toList());
        }

        return builder.build();
    }
}
