package com.frezo.auth.config;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final org.springframework.core.io.ResourceLoader resourceLoader;

    @Override
    public void run(String... args) throws Exception {
        try {
            String targetUsername = "admin";
            String targetPassword = "123456";
            String targetEmail = "admin@frezo.com";

            User admin = userRepository.findByUserName(targetUsername).orElse(null);

            if (admin == null) {
                log.info("User '{}' not found. Creating new admin user...", targetUsername);
                admin = new User();
                admin.setUserName(targetUsername);
                admin.setName("Administrator");
                admin.setEmail(targetEmail);
                admin.setIsDeleted(false);
                admin.setStatus(1);
                admin.setPassword(targetPassword);
                admin.setDataAction((short) 3); // Toàn quyền

                if (admin.getId() == null)
                    admin.setId(java.util.UUID.randomUUID().toString());

                userRepository.save(admin);
            } else {
                log.info("User '{}' found. Updating info...", targetUsername);
                admin.setPassword(targetPassword);
                // Ensure active
                admin.setStatus(1);
                // Ensure full permission
                admin.setDataAction((short) 3);
                userRepository.save(admin);
            }

            // CHECK & CREATE PERSON (Using JDBC to avoid circular dependency with module-qtht)
            if (admin.getPersonId() == null) {

                String personId = null;
                try {
                    // Check if person exists by email
                    String checkPersonSql = "SELECT id FROM person WHERE email = ?";
                    try {
                        personId = jdbcTemplate.queryForObject(checkPersonSql, String.class, targetEmail);
                    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                        // Person not found
                    }

                    if (personId == null) {
                        log.info("Person with email '{}' not found. Creating new Admin Person...", targetEmail);
                        personId = java.util.UUID.randomUUID().toString();

                        String insertPersonSql = """
                            INSERT INTO person (id, code, name, email, activated, is_admin, created_date, created_by, updated_date, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system')
                        """;

                        jdbcTemplate.update(insertPersonSql, personId, "ADMIN", "Administrator", targetEmail, true, true);
                        log.info(">>> SUCCESS: Created Person 'Administrator' (isAdmin=true)");
                    }

                    // Link User to Person
                    admin.setPersonId(personId);
                    userRepository.save(admin);
                    log.info(">>> LINKED: User '{}' to Person '{}'", admin.getUserName(), personId);

                } catch (Exception e) {
                   log.error("Error checking/creating Person for admin", e);
                }
            }

            // CHECK & SEED MENUS
            try {
                // 1. Seed Menus
                log.info("=== CHECKING/SEEDING MENU DATA ===");
                executeSqlScript("classpath:data/menu_data.sql");

                // 2. Seed Roles (ADMIN, MANAGER, STAFF)
                // Always try to seed roles (SQL handles duplicates)
                log.info("=== SEEDING ROLE DATA ===");
                executeSqlScript("classpath:data/role_data.sql");

                // 3. Seed Role-Menu (Full Permission for ADMIN)
                // Always try to update/insert missing permissions for ADMIN
                log.info("=== CHECKING/SEEDING ROLE-MENU DATA FOR ADMIN ===");
                executeSqlScript("classpath:data/role_menu_data.sql");

                // 4. Link User Admin to Role ADMIN
                log.info("=== CHECKING/LINKING ADMIN USER TO ADMIN ROLE ===");
                try {
                     String roleId = jdbcTemplate.queryForObject("SELECT id FROM roles WHERE code = 'ADMIN' AND app_code = 'QTHT'", String.class);
                     if (roleId != null && admin.getId() != null) {
                         Integer userRoleCount = jdbcTemplate.queryForObject(
                             "SELECT COUNT(*) FROM user_role WHERE user_id = ? AND role_id = ?",
                             Integer.class, admin.getId(), roleId
                         );

                         if (userRoleCount != null && userRoleCount == 0) {
                             String insertUserRoleSql = "INSERT INTO user_role (id, user_id, role_id, status, is_delete, created_date, created_by, updated_date, updated_by) VALUES (?, ?, ?, ?, ?, NOW(), 'system', NOW(), 'system')";
                             jdbcTemplate.update(insertUserRoleSql, java.util.UUID.randomUUID().toString(), admin.getId(), roleId, 1, false);
                             log.info(">>> SUCCESS: Linked User 'admin' to Role 'ADMIN'");
                         } else {
                             log.info("User 'admin' already has Role 'ADMIN'. Skipping.");
                         }
                     }
                } catch (Exception e) {
                    log.error("Error linking Admin User to Admin Role", e);
                }

                try {
                    log.info("=== SEEDING ORGANIZATION DATA ===");
                    executeSqlScript("classpath:data/organization_data.sql");
                } catch (Exception e) {
                     log.error("Error seeding organization data", e);
                }

                try {
                    log.info("=== SEEDING CATEGORY DATA ===");
                    executeSqlScript("classpath:data/category_data.sql");
                } catch (Exception e) {
                     log.error("Error seeding category data", e);
                }

            } catch (Exception e) {
                log.error("Error seeding data", e);
            }
        } catch (Exception e) {
            log.error("Failed to initialize admin user", e);
        }
    }

    private void executeSqlScript(String location) {
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource(location);
            if (resource.exists()) {
                String sql = new String(org.springframework.util.StreamUtils.copyToByteArray(resource.getInputStream()), java.nio.charset.StandardCharsets.UTF_8);
                // Split by ; to execute statements
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    if (statement.trim().length() > 0) {
                        try {
                            jdbcTemplate.execute(statement);
                        } catch (Exception e) {
                             log.warn("Failed to execute statement: " + statement, e);
                        }
                    }
                }
                log.info(">>> SUCCESS: Executed {}", location);
            } else {
                log.warn(">>> WARNING: {} not found in classpath", location);
            }
        } catch (Exception e) {
            log.error("Error executing script " + location, e);
        }
    }


}
