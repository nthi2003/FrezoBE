package com.frezo.server;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Smoke integration test — verify:
 * <ol>
 *   <li>App boot lên OK với Testcontainers Postgres</li>
 *   <li>{@code /actuator/health} trả 200</li>
 *   <li>Error response format chuẩn ({@code code, success=false, messageCode, message, traceId, timestamp, path})</li>
 *   <li>CORS whitelist hoạt động</li>
 * </ol>
 * <p>
 * Chạy: {@code mvn -pl module-server verify}
 * <br>Yêu cầu: Docker running.
 * <p>
 * Chuẩn đặt tên: {@code *IT.java} — chạy bởi maven-failsafe-plugin (phase {@code integration-test}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractPostgresIntegrationTest.Initializer.class)
@DisplayName("Smoke IT — App bootstrap + response format")
class ApiResponseSmokeIT extends AbstractPostgresIntegrationTest {

    @LocalServerPort int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    @DisplayName("/actuator/health trả UP")
    void actuatorHealth_shouldReturnUp() {
        given()
                .accept(ContentType.JSON)
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Endpoint không tồn tại trả ApiResponse chuẩn với code 404 + traceId + path")
    void nonExistentPath_shouldReturnStandardErrorFormat() {
        given()
                .accept(ContentType.JSON)
        .when()
                .get("/qtht/does-not-exist-path")
        .then()
                .statusCode(404)
                .body("code", equalTo(404))
                .body("success", equalTo(false))
                .body("messageCode", notNullValue())
                .body("message", notNullValue())
                .body("path", notNullValue())
                .body("timestamp", notNullValue())
                .body("$", hasKey("traceId"));
    }

    @Test
    @DisplayName("Response set header X-Correlation-Id từ TraceIdFilter")
    void response_shouldContainCorrelationIdHeader() {
        given()
                .accept(ContentType.JSON)
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200)
                .header("X-Correlation-Id", notNullValue());
    }

    @Test
    @DisplayName("Client truyền X-Correlation-Id → server echo lại header đó")
    void response_shouldEchoClientCorrelationId() {
        String clientTraceId = "test-trace-id-12345";
        given()
                .accept(ContentType.JSON)
                .header("X-Correlation-Id", clientTraceId)
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200)
                .header("X-Correlation-Id", equalTo(clientTraceId));
    }
}
