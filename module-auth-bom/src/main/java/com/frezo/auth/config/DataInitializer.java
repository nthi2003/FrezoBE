package com.frezo.auth.config;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bootstrap admin/superadmin users + seed menu/role/permission mặc định.
 * <p>
 * <b>Nguyên tắc "User luôn map với Person":</b>
 * <ul>
 *   <li>Mọi {@link User} PHẢI có {@code personId} để authorization (menu, permission, feature check)
 *       lookup được {@code Person.isAdmin}. Nếu {@code personId} rỗng → user sẽ KHÔNG bao giờ được coi
 *       là admin dù đúng nghĩa, vì {@code CheckPermissionAspect}, {@code MenuServiceImpl},
 *       {@code RequireFeatureAspect} đều đọc admin flag qua {@code Person.isAdmin}.</li>
 *   <li>{@link #ensureUserWithPerson} là single source of truth: tạo/rehash User, tạo Person nếu chưa có,
 *       link 2 chiều, set {@code Person.isAdmin} đúng flag mong muốn. Dùng cho cả seed admin, superadmin,
 *       và mọi user "hệ thống" tương lai.</li>
 *   <li>{@link #backfillOrphanUserPersonLinks} chạy 1 lần lúc boot: mọi user cũ có {@code person_id IS NULL}
 *       sẽ được tạo Person tương ứng (mặc định {@code is_admin=false}) và link. Không lỡ để user login xong
 *       bị deny quyền do lookup Person fail.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String SEED_ACTOR = "system";
    private static final short DATA_ACTION_FULL = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) {
        try {
            // 0. Schema guard: password column phải ≥ 100 (DelegatingPasswordEncoder = "{bcrypt}" + 60).
            // Hibernate ddl-auto=update KHÔNG widen cột cũ → rehash bị fail (varchar(50)) và password
            // plain text còn lại → login lỗi "no PasswordEncoder mapped for the id null".
            ensurePasswordColumnWidth();

            // 1. Seed 2 built-in super users: admin + superadmin (Person.isAdmin=true)
            ensureUserWithPerson("admin", "123456", "Administrator", "admin@frezo.com", true);
            ensureUserWithPerson("superadmin", "123456", "Super Administrator", "superadmin@frezo.com", true);

            // 2. Backfill: mọi user cũ chưa có personId → tạo Person + link
            backfillOrphanUserPersonLinks();

            // 3. Seed menu / role / permission / mapping
            seedRbacAndReferenceData();

            // 4. Gán role ADMIN cho các super user (idempotent)
            linkUserToRole("admin", "ADMIN", "QTHT");
            linkUserToRole("superadmin", "ADMIN", "QTHT");

            // 5. Seed organization / category
            safeExecute("classpath:data/organization_data.sql", "organization_data");
            safeExecute("classpath:data/category_data.sql", "category_data");

            // 6. Seed demo/sample data (persons, departments, articles, customers,
            //    products, tasks, tags, email templates, ...) — chạy cuối cùng
            //    vì phụ thuộc org + person "hệ thống" đã có.
            log.info("=== SEEDING DEMO / SAMPLE DATA ===");
            safeExecute("classpath:data/demo_data.sql", "demo_data");
            safeExecute("classpath:data/contract_template_seed.sql", "contract_template_seed");

            // 7. Seed 3 demo login users — password BCrypt encode qua PasswordEncoder Bean
            //    (không hardcode hash trong SQL vì mỗi encoder cấu hình strength khác nhau).
            //    Link với Person đã seed ở demo_data (theo email) → thấy được menu qua RBAC.
            log.info("=== SEEDING DEMO LOGIN USERS ===");
            seedDemoLoginUsers();
        } catch (Exception e) {
            log.error("DataInitializer failed", e);
        }
    }

    // =====================================================================
    // Schema guard
    // =====================================================================

    /**
     * Đảm bảo {@code users.password} đủ rộng cho hash {@code {bcrypt}$2a$10$...} (~68 chars).
     * Idempotent — no-op nếu cột đã ≥ 255.
     */
    private void ensurePasswordColumnWidth() {
        try {
            Integer maxLen = jdbcTemplate.queryForObject(
                    """
                    SELECT character_maximum_length
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'users'
                      AND column_name = 'password'
                    """,
                    Integer.class);
            if (maxLen != null && maxLen < 255) {
                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN password TYPE varchar(255)");
                log.info(">>> Widened users.password varchar({}) → varchar(255) for BCrypt hashes", maxLen);
            }
        } catch (Exception e) {
            log.warn("ensurePasswordColumnWidth skipped: {}", e.getMessage());
        }
    }

    // =====================================================================
    // Core: ensure User + Person + link + isAdmin flag (idempotent)
    // =====================================================================

    /**
     * Tạo/cập nhật User và Person tương ứng, đảm bảo:
     * <ul>
     *   <li>User tồn tại với password BCrypt (auto-rehash nếu password DB còn plain-text hoặc unknown encoder).</li>
     *   <li>Person tồn tại (lookup theo email); nếu chưa có → tạo mới với flag {@code isAdmin}.</li>
     *   <li>{@code User.person_id} luôn point về Person đúng — không để orphan.</li>
     *   <li>Nếu {@code isAdmin=true} → force update Person.is_admin=true kể cả Person đã tồn tại (idempotent).</li>
     * </ul>
     */
    private void ensureUserWithPerson(String username, String rawPassword, String displayName,
                                      String email, boolean isAdmin) {
        try {
            String encodedPassword = passwordEncoder.encode(rawPassword);

            User user = userRepository.findByUserName(username).orElse(null);
            if (user == null) {
                log.info("Bootstrap user '{}' (isAdmin={})...", username, isAdmin);
                user = User.builder()
                        .userName(username)
                        .name(displayName)
                        .email(email)
                        .password(encodedPassword)
                        .status(1)
                        .dataAction(DATA_ACTION_FULL)
                        .build();
                user.setId(UUID.randomUUID().toString());
                user.setIsDeleted(false);
                userRepository.save(user);
            } else {
                if (needRehash(user.getPassword(), rawPassword)) {
                    log.info("Rehashing legacy password for user '{}'.", username);
                    user.setPassword(encodedPassword);
                }
                user.setStatus(1);
                user.setDataAction(DATA_ACTION_FULL);
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    user.setEmail(email);
                }
                userRepository.save(user);
            }

            String personId = ensurePerson(email, displayName, isAdmin);

            if (personId != null && !personId.equals(user.getPersonId())) {
                user.setPersonId(personId);
                userRepository.save(user);
                log.info(">>> LINKED user='{}' → person='{}' (isAdmin={})", username, personId, isAdmin);
            }
        } catch (Exception e) {
            log.error("ensureUserWithPerson failed for username='{}'", username, e);
        }
    }

    /**
     * Password không có prefix {@code {id}} hợp lệ → cần rehash để tránh
     * {@code IllegalArgumentException("There is no PasswordEncoder mapped for the id ...")}
     * khi Spring DelegatingPasswordEncoder cố gắng resolve.
     */
    private boolean needRehash(String currentEncoded, String rawPassword) {
        if (currentEncoded == null || currentEncoded.isBlank()) return true;
        boolean hasKnownPrefix = currentEncoded.matches("^\\{[a-zA-Z0-9]+}.*");
        if (!hasKnownPrefix) return true;
        try {
            return !passwordEncoder.matches(rawPassword, currentEncoded);
        } catch (IllegalArgumentException ex) {
            log.warn("Password has unknown encoder id ({}). Force rehash.", ex.getMessage());
            return true;
        }
    }

    /**
     * Idempotent: lookup Person theo email, tạo mới nếu chưa có; luôn cập nhật flag {@code is_admin}
     * nếu param {@code isAdmin=true} (không hạ cấp Person đang admin thành non-admin).
     *
     * @return id của Person (không bao giờ null nếu insert/update thành công).
     */
    private String ensurePerson(String email, String displayName, boolean isAdmin) {
        try {
            String personId = null;
            try {
                personId = jdbcTemplate.queryForObject(
                        "SELECT id FROM person WHERE email = ? AND (is_deleted = false OR is_deleted IS NULL)",
                        String.class, email);
            } catch (EmptyResultDataAccessException ignore) { /* not found */ }

            if (personId == null) {
                personId = UUID.randomUUID().toString();
                jdbcTemplate.update("""
                                INSERT INTO person (id, code, name, email, activated, is_admin, is_deleted,
                                                    created_date, created_by, updated_date, updated_by)
                                VALUES (?, ?, ?, ?, ?, ?, false,
                                        CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?)
                                """,
                        personId,
                        generatePersonCode(displayName),
                        displayName,
                        email,
                        true,
                        isAdmin,
                        SEED_ACTOR,
                        SEED_ACTOR);
                log.info(">>> CREATED person id={} email='{}' isAdmin={}", personId, email, isAdmin);
            } else if (isAdmin) {
                // Ensure admin flag không bị mất do update thủ công
                jdbcTemplate.update(
                        "UPDATE person SET is_admin = true, updated_date = CURRENT_TIMESTAMP, updated_by = ? WHERE id = ? AND (is_admin IS NULL OR is_admin = false)",
                        SEED_ACTOR, personId);
            }
            return personId;
        } catch (Exception e) {
            log.error("ensurePerson failed for email='{}'", email, e);
            return null;
        }
    }

    private String generatePersonCode(String displayName) {
        if (displayName == null || displayName.isBlank()) return "PS" + System.currentTimeMillis() % 1_000_000;
        String slug = displayName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (slug.length() > 18) slug = slug.substring(0, 18);
        return slug.isBlank() ? "PS" + System.currentTimeMillis() % 1_000_000 : slug;
    }

    // =====================================================================
    // Backfill: user cũ chưa có personId → auto tạo Person + link
    // =====================================================================

    /**
     * Tìm mọi user có {@code person_id IS NULL} và:
     * <ul>
     *   <li>Có email → lookup Person theo email; nếu chưa có → tạo Person mới ({@code is_admin=false}) rồi link.</li>
     *   <li>Không có email → sinh email placeholder {@code <username>@local.frezo} để tránh conflict unique.</li>
     * </ul>
     * Đảm bảo mọi user sau backfill đều thấy quyền menu/permission qua Person.
     */
    private void backfillOrphanUserPersonLinks() {
        try {
            List<Map<String, Object>> orphans = jdbcTemplate.queryForList(
                    "SELECT id, user_name, name, email FROM users WHERE person_id IS NULL AND (is_deleted = false OR is_deleted IS NULL)");

            if (orphans.isEmpty()) {
                log.debug("No orphan users (person_id IS NULL). Skip backfill.");
                return;
            }

            log.info("=== BACKFILL: {} orphan user(s) missing person_id ===", orphans.size());
            for (Map<String, Object> row : orphans) {
                String userId = (String) row.get("id");
                String username = (String) row.get("user_name");
                String displayName = (String) row.getOrDefault("name", username);
                String email = (String) row.get("email");
                if (email == null || email.isBlank()) {
                    email = username + "@local.frezo";
                }
                String personId = ensurePerson(email, displayName != null ? displayName : username, false);
                if (personId != null) {
                    jdbcTemplate.update("UPDATE users SET person_id = ?, updated_date = CURRENT_TIMESTAMP, updated_by = ? WHERE id = ?",
                            personId, SEED_ACTOR, userId);
                    log.info(">>> BACKFILLED user='{}' → person='{}'", username, personId);
                }
            }
        } catch (Exception e) {
            log.error("backfillOrphanUserPersonLinks failed", e);
        }
    }

    // =====================================================================
    // RBAC seed + role linking
    // =====================================================================

    private void seedRbacAndReferenceData() {
        log.info("=== SEEDING MENU DATA (core + extended) ===");
        safeExecute("classpath:data/menu_data.sql", "menu_data");
        safeExecute("classpath:data/menu_data_extended.sql", "menu_data_extended");
        // LNK-07 / MENU-01 SSOT: ONLY menu_tree_v3.sql (9 domain parents).
        // Do NOT load menu_tree_v2.sql / menu_tree_restructure.sql (DEPRECATED archive).
        // See PLAN_LINKAGE_USABILITY.md LNK-07 · docs/BE_MENU_REGROUP_PLAN.md
        safeExecute("classpath:data/menu_tree_v3.sql", "menu_tree_v3");

        log.info("=== SEEDING ROLE DATA ===");
        safeExecute("classpath:data/role_data.sql", "role_data");

        log.info("=== SEEDING PERMISSION CATALOG ===");
        safeExecute("classpath:data/permission_data.sql", "permission_data");

        log.info("=== SEEDING ROLE-MENU MAPPING ===");
        safeExecute("classpath:data/role_menu_data.sql", "role_menu_data");

        log.info("=== SEEDING ROLE-PERMISSION MAPPING ===");
        safeExecute("classpath:data/role_permission_data.sql", "role_permission_data");

        log.info("=== SEEDING MENU-PERMISSION MAPPING ===");
        safeExecute("classpath:data/menu_permission_data.sql", "menu_permission_data");
    }

    private void linkUserToRole(String username, String roleCode, String appCode) {
        try {
            String userId = jdbcTemplate.query(
                    "SELECT id FROM users WHERE user_name = ? LIMIT 1",
                    ps -> ps.setString(1, username),
                    rs -> rs.next() ? rs.getString(1) : null);
            if (userId == null) {
                log.debug("linkUserToRole: user '{}' not found, skip.", username);
                return;
            }

            String roleId = jdbcTemplate.query(
                    "SELECT id FROM roles WHERE code = ? AND app_code = ? LIMIT 1",
                    ps -> {
                        ps.setString(1, roleCode);
                        ps.setString(2, appCode);
                    },
                    rs -> rs.next() ? rs.getString(1) : null);
            if (roleId == null) {
                log.debug("linkUserToRole: role '{}/{}' not found, skip.", roleCode, appCode);
                return;
            }

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_role WHERE user_id = ? AND role_id = ?",
                    Integer.class, userId, roleId);

            if (count == null || count == 0) {
                jdbcTemplate.update("""
                                INSERT INTO user_role (id, user_id, role_id, status, is_deleted,
                                                       created_date, created_by, updated_date, updated_by)
                                VALUES (?, ?, ?, 1, false, NOW(), ?, NOW(), ?)
                                """,
                        UUID.randomUUID().toString(), userId, roleId, SEED_ACTOR, SEED_ACTOR);
                log.info(">>> LINKED role: user='{}' role='{}/{}'", username, roleCode, appCode);
            }
        } catch (Exception e) {
            log.error("linkUserToRole failed user='{}' role='{}/{}'", username, roleCode, appCode, e);
        }
    }

    // =====================================================================
    // Demo login users (linked với Person đã seed trong demo_data.sql)
    // =====================================================================

    /**
     * Tạo 10 demo users login được để test end-to-end mọi luồng nghiệp vụ.
     * <p>
     * Password mặc định của tất cả: <b>123456</b> (BCrypt encode qua {@link PasswordEncoder} Bean).
     * <p>
     * Bảng phân vai:
     * <table>
     *   <tr><th>Username</th><th>Person</th><th>Role</th><th>Nhiệm vụ demo</th></tr>
     *   <tr><td>hungnv</td>  <td>EMP001</td><td>MANAGER</td><td>IT Manager — duyệt Task/Ticket kỹ thuật</td></tr>
     *   <tr><td>maitt</td>   <td>EMP002</td><td>MANAGER</td><td>HR Manager — duyệt LeaveRequest, Contract</td></tr>
     *   <tr><td>tuanle</td>  <td>EMP003</td><td>STAFF</td>  <td>Dev Senior — tạo article DRAFT/WAITING</td></tr>
     *   <tr><td>hapt</td>    <td>EMP004</td><td>STAFF</td>  <td>HR Staff — nộp Contract chờ duyệt</td></tr>
     *   <tr><td>anhhd</td>   <td>EMP005</td><td>MANAGER</td><td>Sales Manager — duyệt Article PUBLIC</td></tr>
     *   <tr><td>bichvn</td>  <td>EMP006</td><td>STAFF</td>  <td>Content Writer — viết bài chờ duyệt</td></tr>
     *   <tr><td>baodq</td>   <td>EMP007</td><td>STAFF</td>  <td>Backend Dev — nhận task</td></tr>
     *   <tr><td>loanbt</td>  <td>EMP008</td><td>MANAGER</td><td>Finance Manager — duyệt Payroll</td></tr>
     *   <tr><td>khangnx</td> <td>EMP009</td><td>STAFF</td>  <td>QA Engineer</td></tr>
     *   <tr><td>trangdt</td> <td>EMP010</td><td>STAFF</td>  <td>Admin Office</td></tr>
     * </table>
     * <p>
     * Tất cả KHÔNG phải admin (Person.is_admin=false) — chỉ có admin/superadmin là admin thực sự.
     * <p>
     * {@link #ensureUserWithPerson} lookup Person theo email và link, không tạo Person trùng
     * (Person EMPxxx đã được seed trong {@code demo_data.sql}).
     */
    private void seedDemoLoginUsers() {
        record DemoUser(String username, String displayName, String email, String roleCode) {}
        List<DemoUser> demos = List.of(
                new DemoUser("hungnv",  "Nguyễn Văn Hùng",  "emp001@frezo.com", "MANAGER"),
                new DemoUser("maitt",   "Trần Thị Mai",     "emp002@frezo.com", "MANAGER"),
                new DemoUser("tuanle",  "Lê Minh Tuấn",     "emp003@frezo.com", "STAFF"),
                new DemoUser("hapt",    "Phạm Thu Hà",      "emp004@frezo.com", "STAFF"),
                new DemoUser("anhhd",   "Hoàng Đức Anh",    "emp005@frezo.com", "MANAGER"),
                new DemoUser("bichvn",  "Vũ Ngọc Bích",     "emp006@frezo.com", "STAFF"),
                new DemoUser("baodq",   "Đặng Quốc Bảo",    "emp007@frezo.com", "STAFF"),
                new DemoUser("loanbt",  "Bùi Thanh Loan",   "emp008@frezo.com", "MANAGER"),
                new DemoUser("khangnx", "Ngô Xuân Khang",   "emp009@frezo.com", "STAFF"),
                new DemoUser("trangdt", "Đỗ Thu Trang",     "emp010@frezo.com", "STAFF")
        );
        for (DemoUser d : demos) {
            ensureUserWithPerson(d.username(), "123456", d.displayName(), d.email(), false);
            linkUserToRole(d.username(), d.roleCode(), "QTHT");
        }
        // LNK-05: ApprovalFlowSeed steps HR / CHIEF_ACC need resolvable users
        linkUserToRole("maitt", "HR", "QTHT");
        linkUserToRole("loanbt", "CHIEF_ACC", "QTHT");
        log.info(">>> Demo login users ready: {} accounts, password '123456'.", demos.size());
    }

    // =====================================================================
    // SQL script executor
    // =====================================================================

    private void safeExecute(String location, String label) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                log.warn(">>> WARNING: {} not found in classpath — skip {}", location, label);
                return;
            }
            DataSource ds = jdbcTemplate.getDataSource();
            if (ds == null) {
                log.error("Cannot execute {} — no DataSource", location);
                return;
            }
            try (Connection conn = ds.getConnection()) {
                ScriptUtils.executeSqlScript(
                        conn,
                        new EncodedResource(resource, StandardCharsets.UTF_8),
                        /* continueOnError */ true,
                        /* ignoreFailedDrops */ true,
                        ScriptUtils.DEFAULT_COMMENT_PREFIX,
                        ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                        ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                        ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
            }
            log.info(">>> SUCCESS: Executed {} ({})", location, label);
        } catch (Exception e) {
            log.error("Error executing script {} ({}): {}", location, label, e.getMessage());
        }
    }
}
