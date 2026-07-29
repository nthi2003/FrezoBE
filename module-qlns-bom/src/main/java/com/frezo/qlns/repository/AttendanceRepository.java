package com.frezo.qlns.repository;

import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByPersonIdAndAttendanceDate(String personId, LocalDate attendanceDate);

    List<Attendance> findByPersonIdAndAttendanceDateBetween(String personId, LocalDate from, LocalDate to);

    List<Attendance> findByAttendanceDateAndIsDeletedFalse(LocalDate attendanceDate);

    /**
     * Đếm ngày công theo khoảng ngày (thay FUNCTION MONTH/YEAR — không tương thích PostgreSQL).
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.personId = :personId " +
           "AND a.attendanceDate >= :from AND a.attendanceDate <= :to " +
           "AND a.status IN (:statuses)")
    int countByPersonIdAndAttendanceDateBetweenAndStatusIn(
            @Param("personId") String personId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") List<AttendanceStatus> statuses);

    @Query("SELECT COALESCE(SUM(a.lateMinutes), 0) FROM Attendance a WHERE a.personId = :personId " +
           "AND a.attendanceDate >= :from AND a.attendanceDate <= :to")
    int sumLateMinutesByPersonIdAndAttendanceDateBetween(
            @Param("personId") String personId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.overtimeMinutes), 0) FROM Attendance a WHERE a.personId = :personId " +
           "AND a.attendanceDate >= :from AND a.attendanceDate <= :to")
    int sumOvertimeMinutesByPersonIdAndAttendanceDateBetween(
            @Param("personId") String personId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT DISTINCT a.personId FROM Attendance a WHERE " +
           "a.attendanceDate >= :from AND a.attendanceDate <= :to")
    List<String> findDistinctPersonIdByAttendanceDateBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("FROM Attendance a WHERE a.attendanceDate >= :from AND a.attendanceDate <= :to")
    List<Attendance> findByAttendanceDateBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
