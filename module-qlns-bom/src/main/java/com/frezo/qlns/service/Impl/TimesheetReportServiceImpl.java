package com.frezo.qlns.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.response.TimesheetReportResponse;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.service.TimesheetReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetReportServiceImpl implements TimesheetReportService {

    private final AttendanceRepository attendanceRepository;
    private final PersonRepository personRepository;

    @Override
    public List<TimesheetReportResponse> getTimesheet(int month, int year, String personId) {
        List<Attendance> records;
        List<String> personIds;

        if (personId != null && !personId.isBlank()) {
            records = attendanceRepository.findByPersonIdAndAttendanceDateBetween(
                    personId, LocalDate.of(year, month, 1), LocalDate.of(year, month, YearMonth.of(year, month).lengthOfMonth()));
            personIds = List.of(personId);
        } else {
            records = attendanceRepository.findByMonthAndYear(month, year);
            personIds = attendanceRepository.findDistinctPersonIdByMonthAndYear(month, year);
        }

        Map<String, List<Attendance>> grouped = records.stream()
                .collect(Collectors.groupingBy(Attendance::getPersonId));

        Map<String, Person> personMap = personRepository.findAllById(personIds).stream()
                .collect(Collectors.toMap(Person::getId, p -> p));

        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();

        List<TimesheetReportResponse> reports = new ArrayList<>();

        for (String pid : personIds) {
            Person person = personMap.get(pid);
            List<Attendance> personRecords = grouped.getOrDefault(pid, Collections.emptyList());
            Map<LocalDate, Attendance> dateMap = personRecords.stream()
                    .collect(Collectors.toMap(Attendance::getAttendanceDate, a -> a, (a, b) -> a));

            TimesheetReportResponse report = new TimesheetReportResponse();
            report.setPersonId(pid);
            report.setPersonCode(person != null ? person.getCode() : "");
            report.setPersonName(person != null ? person.getName() : "");
            report.setMonth(month);
            report.setYear(year);
            report.setTotalDays(daysInMonth);

            List<TimesheetReportResponse.TimesheetDayResponse> details = new ArrayList<>();
            int present = 0, late = 0, absent = 0, leave = 0, half = 0, holiday = 0;
            int lateMin = 0, overtimeMin = 0;

            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = LocalDate.of(year, month, d);
                Attendance att = dateMap.get(date);

                TimesheetReportResponse.TimesheetDayResponse day = new TimesheetReportResponse.TimesheetDayResponse();
                day.setDay(d);

                if (att != null) {
                    AttendanceStatus s = att.getStatus();
                    day.setStatus(s != null ? s.name() : "");
                    day.setCheckIn(att.getCheckInTime() != null ? att.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
                    day.setCheckOut(att.getCheckOutTime() != null ? att.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
                    day.setWorkMinutes(att.getWorkMinutes());
                    day.setLateMinutes(att.getLateMinutes());

                    if (s != null) {
                        switch (s) {
                            case PRESENT -> present++;
                            case LATE -> { late++; lateMin += att.getLateMinutes() != null ? att.getLateMinutes() : 0; }
                            case ABSENT -> absent++;
                            case LEAVE -> leave++;
                            case HALF_DAY -> half++;
                            case HOLIDAY -> holiday++;
                        }
                        if (att.getOvertimeMinutes() != null) overtimeMin += att.getOvertimeMinutes();
                    }
                } else {
                    day.setStatus("NO_DATA");
                }
                details.add(day);
            }

            report.setPresentDays(present);
            report.setLateDays(late);
            report.setAbsentDays(absent);
            report.setLeaveDays(leave);
            report.setHalfDays(half);
            report.setHolidayDays(holiday);
            report.setTotalLateMinutes(lateMin);
            report.setTotalOvertimeMinutes(overtimeMin);
            report.setDetails(details);
            reports.add(report);
        }

        reports.sort(Comparator.comparing(TimesheetReportResponse::getPersonCode));
        return reports;
    }

    @Override
    public byte[] exportTimesheetExcel(int month, int year, String personId) {
        List<TimesheetReportResponse> reports = getTimesheet(month, year, personId);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bang cham cong T" + month + "_" + year);

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            YearMonth yearMonth = YearMonth.of(year, month);
            int daysInMonth = yearMonth.lengthOfMonth();

            // Row 0: Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG CHẤM CÔNG THÁNG " + month + "/" + year);
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Row 1: Headers
            Row header = sheet.createRow(1);
            String[] cols = {"STT", "Mã NV", "Họ tên"};
            for (int d = 1; d <= daysInMonth; d++) {
                header.createCell(2 + d - 1).setCellValue(String.valueOf(d));
            }
            header.createCell(2 + daysInMonth).setCellValue("Tổng");

            int lastCol = 2 + daysInMonth;
            for (int i = 0; i <= lastCol; i++) {
                Cell cell = header.getCell(i);
                if (cell != null) cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 2;
            int stt = 0;
            for (TimesheetReportResponse report : reports) {
                stt++;
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stt);
                row.createCell(1).setCellValue(report.getPersonCode());
                row.createCell(2).setCellValue(report.getPersonName());

                int workingDays = 0;
                for (int d = 0; d < daysInMonth; d++) {
                    if (d < report.getDetails().size()) {
                        TimesheetReportResponse.TimesheetDayResponse day = report.getDetails().get(d);
                        String status = day.getStatus();
                        String display = switch (status) {
                            case "PRESENT" -> "X";
                            case "LATE" -> "M";
                            case "ABSENT" -> "";  // blank for absent
                            case "LEAVE" -> "P";
                            case "HALF_DAY" -> "1/2";
                            case "HOLIDAY" -> "L";
                            default -> "";
                        };
                        row.createCell(3 + d).setCellValue(display);
                        if (!"ABSENT".equals(status) && !"NO_DATA".equals(status)) {
                            workingDays++;
                        }
                    }
                }
                row.createCell(lastCol).setCellValue(workingDays);
            }

            // Summary row
            rowNum++;
            Row summary = sheet.createRow(rowNum);
            summary.createCell(0).setCellValue("TỔNG");
            int totalEmployees = reports.size();
            summary.createCell(1).setCellValue(totalEmployees + " NV");
            for (int d = 0; d < daysInMonth; d++) {
                int presentCount = 0;
                for (TimesheetReportResponse report : reports) {
                    if (d < report.getDetails().size()) {
                        String s = report.getDetails().get(d).getStatus();
                        if (s != null && !"ABSENT".equals(s) && !"NO_DATA".equals(s)) {
                            presentCount++;
                        }
                    }
                }
                summary.createCell(3 + d).setCellValue(presentCount);
            }

            for (int i = 0; i <= lastCol; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new QTHTException("Bảng chấm công xuất Excel thất bại", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
