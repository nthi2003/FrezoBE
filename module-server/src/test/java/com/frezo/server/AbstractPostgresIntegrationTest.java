package com.frezo.server;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class cho integration test — spin PostgreSQL 15 qua Testcontainers.
 * <p>
 * Yêu cầu: Docker phải chạy trên máy chạy test.
 * <p>
 * Cách dùng:
 * <pre>
 * &#64;SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * &#64;ContextConfiguration(initializers = AbstractPostgresIntegrationTest.Initializer.class)
 * class DepartmentControllerIT extends AbstractPostgresIntegrationTest {
 *     // ... test methods
 * }
 * </pre>
 * <p>
 * Container reuse: singleton static field — tất cả test class share 1 container để chạy nhanh.
 * Config {@code .withReuse(true)} + file {@code ~/.testcontainers.properties} có {@code testcontainers.reuse.enable=true}.
 */
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("frezo_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /**
     * Spring context initializer — inject DB properties từ container lúc runtime.
     * Đảm bảo Testcontainers-provisioned DB được app kết nối thay vì hardcoded jdbc URL.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext ctx) {
            if (!POSTGRES.isRunning()) POSTGRES.start();
            TestPropertyValues.of(
                    "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                    "spring.datasource.username=" + POSTGRES.getUsername(),
                    "spring.datasource.password=" + POSTGRES.getPassword(),
                    "spring.datasource.driver-class-name=org.postgresql.Driver",
                    "spring.jpa.hibernate.ddl-auto=create-drop",       // fresh schema mỗi run
                    "spring.flyway.enabled=false",
                    "app.security.jwt.secret=integration-test-only-jwt-secret-64bytes-frezoBE-frezoBE-frezoBE!!",
                    "app.security.encryption-key=integration-test-encryption-key-32chars-frezo"
            ).applyTo(ctx.getEnvironment());
        }
    }
}
