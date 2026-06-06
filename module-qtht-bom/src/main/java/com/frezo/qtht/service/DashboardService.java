package com.frezo.qtht.service;

// import com.frezo.qlns.dto.response.DashboardSummaryResponse;
// import com.frezo.qlns.repository.AttendanceRepository;
// import com.frezo.qlns.repository.ContractRepository;
// import com.frezo.qtn.repository.TaskRepository;
// import com.frezo.qtbv.repository.ArticleRepository;
import com.frezo.qtht.dto.response.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // private final TaskRepository taskRepository;
    // private final AttendanceRepository attendanceRepository;
    // private final ArticleRepository articleRepository;
    // private final ContractRepository contractRepository;

    public DashboardSummaryResponse getSummary() {
        return DashboardSummaryResponse.builder()
            .pendingTasks(0L)
            .todayAttendance(0L)
            .newArticles(0L)
            .expiringContracts(new ArrayList<>())
            .build();
    }
}
