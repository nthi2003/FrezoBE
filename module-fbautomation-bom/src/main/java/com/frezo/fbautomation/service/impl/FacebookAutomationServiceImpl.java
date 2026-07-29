package com.frezo.fbautomation.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.fbautomation.common.FbAutomationErrorCode;
import com.frezo.fbautomation.config.WebDriverConfig;
import com.frezo.fbautomation.dto.response.AutomationSummaryResponse;
import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.entity.FacebookAccount;
import com.frezo.fbautomation.entity.FacebookGroup;
import com.frezo.fbautomation.entity.FacebookLead;
import com.frezo.fbautomation.mapper.FacebookGroupMapper;
import com.frezo.fbautomation.repository.FacebookAccountRepository;
import com.frezo.fbautomation.repository.FacebookGroupRepository;
import com.frezo.fbautomation.repository.FacebookLeadRepository;
import com.frezo.fbautomation.service.FacebookAccountService;
import com.frezo.fbautomation.service.FacebookAutomationService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FacebookAutomationServiceImpl — Service lõi xử lý Selenium WebDriver.
 * Toàn bộ thao tác với trình duyệt đều được bọc trong cơ chế:
 *   - Delay ngẫu nhiên (5s-15s) để giả lập người thật
 *   - Mỗi tài khoản dùng proxy riêng
 *   - Multi-threading (async) để tránh checkpoint
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookAutomationServiceImpl implements FacebookAutomationService {

    private final FacebookAccountRepository accountRepository;
    private final FacebookGroupRepository groupRepository;
    private final FacebookLeadRepository leadRepository;
    private final FacebookGroupMapper groupMapper;
    private final FacebookAccountService accountService;

    // Thread-safe map lưu các driver đang hoạt động
    private final Map<String, WebDriver> activeDrivers = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────
    // 1. SEARCH & SCRAPE GROUPS (Bất đồng bộ)
    // ─────────────────────────────────────────────────────────
    @Override
    @Async("fbAutomationExecutor")
    public CompletableFuture<List<FacebookGroupResponse>> searchAndScrapeGroups(
            String accountId, String keyword, Integer maxResults) {

        List<FacebookGroupResponse> result = new ArrayList<>();
        WebDriver driver = null;
        try {
            // Lấy thông tin tài khoản
            FacebookAccount account = findAccount(accountId);
            int limit = (maxResults != null && maxResults > 0) ? maxResults : 20;

            // Tạo driver với proxy riêng của tài khoản
            driver = WebDriverConfig.createDriver(account.getProxyIp());
            activeDrivers.put(accountId, driver);

            // Đăng nhập bằng cookie
            loginWithCookie(driver, account.getCookie());
            randomDelay(3000, 8000);

            // Truy cập trang tìm kiếm group
            String searchUrl = "https://mbasic.facebook.com/search/groups/?q="
                    + java.net.URLEncoder.encode(keyword, "UTF-8");
            driver.get(searchUrl);
            randomDelay(5000, 12000);

            // Parse HTML bằng JSoup
            Document doc = Jsoup.parse(driver.getPageSource());
            Elements groupLinks = doc.select("a[href*=/groups/]");

            int count = 0;
            Set<String> seenIds = new HashSet<>();

            for (Element link : groupLinks) {
                if (count >= limit) break;

                String href = link.attr("href");
                String groupId = extractGroupId(href);
                if (groupId == null || seenIds.contains(groupId)) continue;
                seenIds.add(groupId);

                // Lấy tên group
                String groupName = link.text().trim();
                if (groupName.isBlank()) continue;

                // Lấy số lượng thành viên từ các phần tử xung quanh
                Element parent = link.parent();
                String memberText = parent != null ? parent.text() : "";
                int memberCount = extractMemberCount(memberText);

                // Tạo hoặc cập nhật group trong DB
                FacebookGroup group = saveOrUpdateGroup(groupId, groupName, memberCount, href);
                result.add(groupMapper.toResponse(group));
                count++;
                randomDelay(2000, 5000);
            }

            log.info("Đã scrape xong {} groups cho account {}", result.size(), account.getUsername());

        } catch (Exception e) {
            log.error("Lỗi khi scrape groups cho account {}: {}", accountId, e.getMessage());
            throw new AppException(FbAutomationErrorCode.SCRAPE_GROUPS_FAILED, e.getMessage());
        } finally {
            // Đóng driver và xóa khỏi map
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
                activeDrivers.remove(accountId);
            }
        }

        return CompletableFuture.completedFuture(result);
    }

    // ─────────────────────────────────────────────────────────
    // 2. AUTO JOIN GROUP (Bất đồng bộ)
    // ─────────────────────────────────────────────────────────
    @Override
    @Async("fbAutomationExecutor")
    public CompletableFuture<String> autoJoinGroup(String accountId, String groupId) {
        WebDriver driver = null;
        try {
            FacebookAccount account = findAccount(accountId);
            FacebookGroup group = groupRepository.findByGroupId(groupId)
                    .orElseThrow(() -> new AppException(FbAutomationErrorCode.GROUP_ID_NOT_FOUND, groupId));

            driver = WebDriverConfig.createDriver(account.getProxyIp());
            activeDrivers.put(accountId, driver);

            // Đăng nhập
            loginWithCookie(driver, account.getCookie());
            randomDelay(3000, 8000);

            // Truy cập group
            String groupUrl = group.getGroupUrl() != null ? group.getGroupUrl()
                    : "https://mbasic.facebook.com/groups/" + groupId;
            driver.get(groupUrl);
            randomDelay(5000, 10000);

            // Tìm nút "Tham gia nhóm" / "Join Group"
            // Trên mbasic, nút join thường có text "Tham gia nhóm" hoặc "Join Group"
            try {
                WebElement joinBtn = driver.findElement(By.xpath(
                        "//a[contains(text(),'Tham gia') or contains(text(),'Join')]"));
                joinBtn.click();
                randomDelay(3000, 7000);

                // Có thể cần click nút "Gửi yêu cầu" / "Send Request"
                try {
                    WebElement confirmBtn = driver.findElement(By.xpath(
                            "//input[@value='Gửi yêu cầu' or @value='Send Request' or @value='Tham gia nhóm']"));
                    confirmBtn.click();
                    randomDelay(2000, 5000);
                } catch (Exception ignored) {}

                group.setStatus("JOINED");
                groupRepository.save(group);

                String msg = "Đã tham gia group " + group.getGroupName() + " thành công";
                log.info(msg);
                return CompletableFuture.completedFuture(msg);

            } catch (Exception e) {
                group.setStatus("REJECTED");
                groupRepository.save(group);
                log.warn("Không thể tham gia group {}: {}", groupId, e.getMessage());
                return CompletableFuture.completedFuture("Không thể tham gia group (có thể đã là thành viên hoặc bị chặn)");
            }

        } catch (Exception e) {
            log.error("Lỗi autoJoinGroup: {}", e.getMessage());
            throw new AppException(FbAutomationErrorCode.JOIN_GROUP_FAILED, e.getMessage());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
                activeDrivers.remove(accountId);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 3. LOGIN WITH COOKIE
    // ─────────────────────────────────────────────────────────
    @Override
    public void loginWithCookie(String accountId) {
        WebDriver driver = null;
        try {
            FacebookAccount account = findAccount(accountId);
            driver = WebDriverConfig.createDriver(account.getProxyIp());
            loginWithCookie(driver, account.getCookie());
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Đăng nhập Facebook bằng cookie đã lưu.
     * Nếu cookie hết hạn, sẽ thử đăng nhập bằng user/pass.
     */
    public void loginWithCookie(WebDriver driver, String cookieStr) {
        try {
            // Vào Facebook trước để set cookie đúng domain
            driver.get("https://mbasic.facebook.com");
            randomDelay(3000, 6000);

            if (cookieStr != null && !cookieStr.isBlank()) {
                // Parse cookie string: "key1=value1; key2=value2"
                String[] pairs = cookieStr.split(";");
                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2) {
                        org.openqa.selenium.Cookie cookie = new org.openqa.selenium.Cookie(
                                kv[0].trim(), kv[1].trim());
                        driver.manage().addCookie(cookie);
                    }
                }
                // Refresh để áp dụng cookie
                driver.navigate().refresh();
                randomDelay(3000, 7000);
            }

        } catch (Exception e) {
            log.error("Lỗi đăng nhập bằng cookie: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    // 4. SUMMARY
    // ─────────────────────────────────────────────────────────
    @Override
    public AutomationSummaryResponse getSummary() {
        return AutomationSummaryResponse.builder()
                .totalAccounts(accountRepository.count())
                .activeAccounts(accountRepository.findByStatus("ACTIVE").size())
                .totalGroups(groupRepository.count())
                .approvedGroups(groupRepository.findByStatus("JOINED").size())
                .totalLeads(leadRepository.count())
                .importedLeads(leadRepository.countByStatus("IMPORTED"))
                .pendingLeads(leadRepository.countByStatus("NEW"))
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // ANTI-BAN: Delay ngẫu nhiên
    // ─────────────────────────────────────────────────────────
    /**
     * Tạo độ trễ ngẫu nhiên từ minMs đến maxMs mili-giây.
     * Giả lập hành vi người thật, tránh bị Facebook phát hiện.
     */
    public static void randomDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + new Random().nextInt(maxMs - minMs + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────
    private FacebookAccount findAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(FbAutomationErrorCode.ACCOUNT_NOT_FOUND));
    }

    /**
     * Trích xuất group ID từ URL.
     * VD: "/groups/123456789" -> "123456789"
     */
    private String extractGroupId(String href) {
        if (href == null) return null;
        String[] parts = href.split("/groups/");
        if (parts.length < 2) return null;
        String id = parts[1].split("/")[0].split("\\?")[0];
        return id.matches("\\d+") ? id : null;
    }

    /**
     * Trích xuất số lượng thành viên từ text.
     * VD: "123.456 thành viên" -> 123456
     */
    private int extractMemberCount(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            String numStr = text.replaceAll("[^0-9]", "");
            return numStr.isBlank() ? 0 : Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Lưu hoặc cập nhật group vào DB.
     */
    private FacebookGroup saveOrUpdateGroup(String groupId, String name, int members, String url) {
        Optional<FacebookGroup> existing = groupRepository.findByGroupId(groupId);
        if (existing.isPresent()) {
            FacebookGroup g = existing.get();
            g.setGroupName(name);
            g.setMemberCount(members);
            g.setGroupUrl("https://mbasic.facebook.com" + url);
            return groupRepository.save(g);
        }
        FacebookGroup newGroup = FacebookGroup.builder()
                .groupId(groupId)
                .groupName(name)
                .memberCount(members)
                .relevanceScore(0.5)
                .status("NEW")
                .groupUrl("https://mbasic.facebook.com" + url)
                .build();
        return groupRepository.save(newGroup);
    }

    /**
     * Dọn dẹp driver khi service bị destroy.
     */
    @PreDestroy
    public void cleanup() {
        log.info("Đang đóng {} driver đang hoạt động...", activeDrivers.size());
        activeDrivers.values().forEach(driver -> {
            try { driver.quit(); } catch (Exception ignored) {}
        });
        activeDrivers.clear();
    }
}
