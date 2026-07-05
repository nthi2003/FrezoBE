package com.frezo.fbautomation.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * WebDriverConfig — Cấu hình Selenium ChromeDriver với chế độ ẩn danh,
 * tắt WebRTC, tắt hình ảnh, fake User-Agent di động để giảm tải RAM
 * cho Server và tránh bị phát hiện bot.
 */
@Slf4j
@Configuration
public class WebDriverConfig {

    private static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36";

    /**
     * Tạo ChromeOptions mặc định với cấu hình chống phát hiện bot.
     * Gọi createDefaultOptions() để lấy bản sao mới mỗi lần.
     */
    public static ChromeOptions createDefaultOptions() {
        ChromeOptions options = new ChromeOptions();

        // ── 1. Chế độ ẩn danh & chống phát hiện ──────────────────────
        options.addArguments("--incognito");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-automation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // ── 2. Tắt WebRTC (chống rò rỉ IP thật) ─────────────────────
        options.addArguments("--disable-webrtc");
        options.addArguments("--enforce-webrtc-ip-permission-check");
        options.addArguments("--force-webrtc-ip-handling-policy=disable_non_proxied_udp");

        // ── 3. Tắt hình ảnh (giảm RAM/CPU) ──────────────────────────
        options.addArguments("--blink-settings=imagesEnabled=false");

        // ── 4. Fake User-Agent di động ──────────────────────────────
        options.addArguments("--user-agent=" + MOBILE_USER_AGENT);

        // ── 5. Preferences bổ sung ──────────────────────────────────
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_setting_values.geolocation", 2);
        prefs.put("profile.default_content_setting_values.media_stream", 2);
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // ── 6. Exclude Switches (ẩn dấu hiệu automation) ────────────
        options.setExperimentalOption("excludeSwitches",
                java.util.List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    /**
     * Tạo ChromeOptions với proxy riêng cho từng tài khoản.
     * @param proxyIp Địa chỉ proxy, VD: "http://user:pass@1.2.3.4:8080"
     */
    public static ChromeOptions createOptionsWithProxy(String proxyIp) {
        ChromeOptions options = createDefaultOptions();
        if (proxyIp != null && !proxyIp.isBlank()) {
            options.addArguments("--proxy-server=" + proxyIp);
            log.debug("Gán proxy: {}", proxyIp);
        }
        return options;
    }

    /**
     * Tạo một WebDriver instance mới với proxy tuỳ chỉnh.
     * Mỗi luồng (thread) nên có một WebDriver riêng.
     */
    public static WebDriver createDriver(String proxyIp) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = (proxyIp != null && !proxyIp.isBlank())
                ? createOptionsWithProxy(proxyIp)
                : createDefaultOptions();
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(412, 915));
        return driver;
    }
}
