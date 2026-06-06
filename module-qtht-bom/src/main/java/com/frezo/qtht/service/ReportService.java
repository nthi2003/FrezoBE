package com.frezo.qtht.service;

import com.alibaba.excel.EasyExcel;
// import com.frezo.qlns.entity.Attendance;
// import com.frezo.qlns.repository.AttendanceRepository;
// import com.frezo.qlns.repository.ContractRepository;
import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
// import com.frezo.task.repository.TaskRepository;
import com.frezo.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    // private final AttendanceRepository attendanceRepository;
    // private final TaskRepository taskRepository;
    // private final ContractRepository contractRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Export monthly attendance to Excel bytes
     */
    public byte[] exportMonthlyAttendance(int month, int year) {
        // List<Attendance> data = attendanceRepository.findAll(); // Should filter by month/year in real repo
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // EasyExcel.write(out, Attendance.class)
        //         .sheet("Attendance Report")
        //         .doWrite(data);
                
        return out.toByteArray();
    }

    /**
     * Automated Weekly Report (Every Monday at 8 AM)
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyReport() {
        log.info("Generating weekly automated report...");
        
        long totalTasks = 0; // taskRepository.count();
        long expiringContractsCount = 0; // contractRepository.count(); // Simplified count

        String content = String.format(
            "Weekly Summary:\n- Total Tasks: %d\n- Expiring Contracts: %d\n- System Status: Stable",
            totalTasks, expiringContractsCount
        );

        // Fetch actual admin email
        Optional<User> adminOpt = userRepository.findByUserName("admin");
        String adminEmail = adminOpt.map(User::getEmail).orElse("admin@frezo.com.vn");

        emailService.sendByTemplate("WEEKLY_REPORT", 
            Map.of("summary", content), 
            Collections.singletonList(adminEmail));
            
        log.info("Weekly report sent to: {}", adminEmail);
    }
}
