package com.frezo.qlns.config;

import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.ContractTemplate;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.repository.ContractTemplateRepository;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seed template + đảm bảo mỗi EMP* có ≥1 HĐ {@code status=ACTIVE} và {@code activated=true}.
 * Idempotent. Profile {@code local}/{@code seed}. Order 80.
 */
@Slf4j
@Component
@Order(80)
@Profile({"local", "seed"})
@RequiredArgsConstructor
public class ContractDataInitializer implements ApplicationRunner {

    private final ContractTemplateRepository templateRepository;
    private final ContractRepository contractRepository;
    private final PersonRepository personRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                seedTemplates();
                ensureActivatedContracts();
            });
        } catch (Exception ex) {
            log.error("[contract-seed] failed — app continues", ex);
        }
    }

    private void seedTemplates() {
        seedTemplate("Mẫu HĐ thử việc", "THU_VIEC",
                "<h2>HỢP ĐỒNG THỬ VIỆC</h2><p>Bên A: {{employerName}} — Bên B: {{personName}}</p>");
        seedTemplate("Mẫu HĐ chính thức (không xác định TH)", "CHINH_THUC",
                "<h2>HỢP ĐỒNG LAO ĐỘNG</h2><p>Bên A: {{employerName}} — Bên B: {{personName}}</p>");
        seedTemplate("Mẫu HĐ thời vụ / xác định TH", "THOI_VU",
                "<h2>HỢP ĐỒNG XÁC ĐỊNH THỜI HẠN</h2><p>{{startDate}} → {{endDate}}</p>");
    }

    private void seedTemplate(String name, String type, String html) {
        for (ContractTemplate t : templateRepository.findByIsDeletedFalseOrderByCreatedDateDesc()) {
            if (type.equalsIgnoreCase(t.getType()) || name.equalsIgnoreCase(t.getName())) {
                return;
            }
        }
        ContractTemplate t = new ContractTemplate();
        t.setId(UUID.randomUUID().toString());
        t.setName(name);
        t.setType(type);
        t.setFileUrl(html);
        t.setFileObjectName("seed/" + type.toLowerCase() + ".html");
        templateRepository.save(t);
        log.info("[contract-seed] template {} ({})", name, type);
    }

    /**
     * EMP* thiếu HĐ ACTIVE+activated → set cả hai trên 1 HĐ live, hoặc tạo/reuse {@code HD_ACTIVE_*}.
     */
    private void ensureActivatedContracts() {
        List<Person> employees = new ArrayList<>();
        for (Person p : personRepository.findAll()) {
            if (!isLive(p.getIsDeleted())) continue;
            if (p.getCode() == null || !p.getCode().startsWith("EMP")) continue;
            employees.add(p);
        }
        if (employees.isEmpty()) {
            log.warn("[contract-seed] no EMP* — skip");
            return;
        }

        Set<String> hasOk = new HashSet<>();
        Map<String, Contract> anyLiveByPerson = new HashMap<>();
        Map<String, Contract> byCode = new HashMap<>();

        for (Contract c : contractRepository.findAll()) {
            if (c.getCode() != null) {
                byCode.put(c.getCode(), c);
            }
            if (!isLive(c.getIsDeleted()) || c.getPersonId() == null) continue;
            if (isActiveActivated(c)) {
                hasOk.add(c.getPersonId());
            }
            anyLiveByPerson.putIfAbsent(c.getPersonId(), c);
        }

        int updated = 0;
        int created = 0;
        int skipped = 0;
        int i = 0;

        for (Person p : employees) {
            if (hasOk.contains(p.getId())) {
                skipped++;
                continue;
            }

            Contract target = anyLiveByPerson.get(p.getId());
            String code = "HD_ACTIVE_" + p.getCode();

            if (target == null) {
                target = byCode.get(code);
                if (target != null) {
                    // orphan / person lệch — gán lại + activate
                    target.setPersonId(p.getId());
                } else {
                    Contract c = newMinimalActive(p, code, i);
                    contractRepository.save(c);
                    hasOk.add(p.getId());
                    byCode.put(code, c);
                    created++;
                    log.info("[contract-seed] created {} → {}", code, p.getCode());
                    i++;
                    continue;
                }
            }

            target.setStatus(StatusContarct.ACTIVE);
            target.setActivated(true);
            contractRepository.save(target);
            hasOk.add(p.getId());
            updated++;
            log.info("[contract-seed] activated {} → {}", target.getCode(), p.getCode());
            i++;
        }

        log.info("[contract-seed] done — created={}, updated={}, alreadyOk={}, empTotal={}",
                created, updated, skipped, employees.size());
    }

    private static Contract newMinimalActive(Person p, String code, int i) {
        Contract c = new Contract();
        c.setId(UUID.randomUUID().toString());
        c.setCode(code);
        c.setName("HĐLĐ đang hiệu lực — " + (p.getName() != null ? p.getName() : p.getCode()));
        c.setPersonId(p.getId());
        c.setTypeContractId("TYPE_ACTIVE_CHINH_THUC_" + p.getCode());
        c.setEffFrom(LocalDate.of(2024, 1, 1).plusMonths(i % 12));
        c.setEffTo(LocalDate.of(2099, 12, 31));
        c.setValue(18_000_000 + i * 1_500_000);
        c.setStatus(StatusContarct.ACTIVE);
        c.setActivated(true);
        c.setEmployerName("Tổng Công ty Công nghệ FTECH");
        c.setEmployerAddress("Tầng 10, FTECH Tower, Cầu Giấy, Hà Nội");
        c.setEmployerTaxCode("0101234567");
        c.setJobPosition("Nhân viên");
        c.setWorkLocation("Hà Nội");
        c.setProbationDays(0);
        c.setHtmlContract("<h1>HỢP ĐỒNG LAO ĐỘNG</h1><p>Số: " + code + "</p>");
        c.setAiStatus("NONE");
        return c;
    }

    private static boolean isActiveActivated(Contract c) {
        return Boolean.TRUE.equals(c.getActivated()) && c.getStatus() == StatusContarct.ACTIVE;
    }

    private static boolean isLive(Boolean isDeleted) {
        return isDeleted == null || Boolean.FALSE.equals(isDeleted);
    }
}
