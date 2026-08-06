package com.frezo.qlns.service.Impl;

import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.dto.response.PersonStatisticsResponse;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.service.PersonStatisticsService;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonStatisticsServiceImpl implements PersonStatisticsService {

    private final PersonRepository personRepository;
    private final ContractRepository contractRepository;

    @Override
    @Transactional(readOnly = true)
    public PersonStatisticsResponse getStatistics(LocalDate from, LocalDate to) {
        LocalDate periodFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate periodTo = to != null ? to : LocalDate.now();

        Specification<Person> base = Specification
                .where((root, q, cb) -> cb.or(
                        cb.isFalse(root.get("isDeleted")),
                        cb.isNull(root.get("isDeleted"))));

        List<Person> all = personRepository.findAll(base);
        long total = all.size();
        long male = all.stream().filter(p -> "MALE".equalsIgnoreCase(p.getGender())).count();
        long female = all.stream().filter(p -> "FEMALE".equalsIgnoreCase(p.getGender())).count();
        long newHires = all.stream()
                .filter(p -> p.getJoinDate() != null
                        && !p.getJoinDate().isBefore(periodFrom)
                        && !p.getJoinDate().isAfter(periodTo))
                .count();
        long resigned = all.stream()
                .filter(p -> Boolean.FALSE.equals(p.getActivated())
                        || (p.getResignDate() != null
                        && !p.getResignDate().isBefore(periodFrom)
                        && !p.getResignDate().isAfter(periodTo)))
                .count();

        List<Contract> contracts = contractRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .toList();

        Set<String> officialPersonIds = new HashSet<>();
        List<Map<String, Object>> probation = new ArrayList<>();
        List<Map<String, Object>> officialList = new ArrayList<>();
        List<Map<String, Object>> contractExpiring = new ArrayList<>();

        LocalDate expiringBefore = LocalDate.now().plusDays(30);

        for (Contract c : contracts) {
            if (c.getPersonId() == null) continue;
            Person person = all.stream().filter(p -> p.getId().equals(c.getPersonId())).findFirst().orElse(null);
            if (person == null) continue;

            boolean isActive = Boolean.TRUE.equals(c.getActivated())
                    && c.getStatus() == StatusContarct.ACTIVE;
            boolean isProbation = c.getProbationDays() != null && c.getProbationDays() > 0
                    && c.getEffFrom() != null
                    && c.getEffFrom().plusDays(c.getProbationDays()).isAfter(LocalDate.now());

            Map<String, Object> row = personRow(person, c);
            if (isActive && isProbation) {
                probation.add(row);
            } else if (isActive) {
                officialPersonIds.add(person.getId());
                officialList.add(row);
                if (c.getEffTo() != null
                        && !c.getEffTo().isBefore(LocalDate.now())
                        && !c.getEffTo().isAfter(expiringBefore)) {
                    row = new HashMap<>(row);
                    row.put("daysUntilExpiry", ChronoUnit.DAYS.between(LocalDate.now(), c.getEffTo()));
                    contractExpiring.add(row);
                }
            }
        }

        List<Map<String, Object>> resignedList = all.stream()
                .filter(p -> Boolean.FALSE.equals(p.getActivated())
                        || p.getResignDate() != null)
                .map(p -> personRow(p, null))
                .toList();

        int month = LocalDate.now().getMonthValue();
        List<Map<String, Object>> birthdays = all.stream()
                .filter(p -> p.getDob() != null && p.getDob().getMonthValue() == month)
                .map(p -> personRow(p, null))
                .toList();

        return PersonStatisticsResponse.builder()
                .total(total)
                .male(male)
                .female(female)
                .newHires(newHires)
                .official(officialPersonIds.size())
                .resigned(resigned)
                .contractExpiring(contractExpiring)
                .probation(probation)
                .officialList(officialList)
                .resignedList(resignedList)
                .birthdays(birthdays)
                .build();
    }

    private Map<String, Object> personRow(Person p, Contract c) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", p.getId());
        row.put("code", p.getCode());
        row.put("name", p.getName());
        row.put("email", p.getEmail());
        row.put("gender", p.getGender());
        row.put("jobTitle", p.getJobTitle());
        row.put("departmentName", p.getDepartment() != null ? p.getDepartment().getName() : null);
        row.put("activated", p.getActivated());
        row.put("joinDate", p.getJoinDate());
        row.put("resignDate", p.getResignDate());
        row.put("birthDate", p.getDob());
        if (c != null) {
            row.put("contractCode", c.getCode());
            row.put("contractEnd", c.getEffTo());
            row.put("contractStatus", c.getStatus() != null ? c.getStatus().name() : null);
        }
        return row;
    }
}
