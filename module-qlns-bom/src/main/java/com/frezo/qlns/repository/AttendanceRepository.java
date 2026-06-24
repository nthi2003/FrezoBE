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

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.personId = :personId " +
           "AND FUNCTION('MONTH', a.attendanceDate) = :month " +
           "AND FUNCTION('YEAR', a.attendanceDate) = :year " +
           "AND a.status IN (:statuses)")
    int countByPersonIdAndMonthAndYearAndStatusIn(
            @Param("personId") String personId,
            @Param("month") int month,
            @Param("year") int year,
            @Param("statuses") List<AttendanceStatus> statuses);

    @Query("SELECT COALESCE(SUM(a.lateMinutes), 0) FROM Attendance a WHERE a.personId = :personId " +
           "AND FUNCTION('MONTH', a.attendanceDate) = :month " +
           "AND FUNCTION('YEAR', a.attendanceDate) = :year")
    int sumLateMinutesByPersonIdAndMonthAndYear(
            @Param("personId") String personId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT COALESCE(SUM(a.overtimeMinutes), 0) FROM Attendance a WHERE a.personId = :personId " +
           "AND FUNCTION('MONTH', a.attendanceDate) = :month " +
           "AND FUNCTION('YEAR', a.attendanceDate) = :year")
    int sumOvertimeMinutesByPersonIdAndMonthAndYear(
            @Param("personId") String personId,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT DISTINCT a.personId FROM Attendance a WHERE " +
           "FUNCTION('MONTH', a.attendanceDate) = :month AND " +
           "FUNCTION('YEAR', a.attendanceDate) = :year")
    List<String> findDistinctPersonIdByMonthAndYear(
            @Param("month") int month,
            @Param("year") int year);

    @Query("FROM Attendance a WHERE " +
           "FUNCTION('MONTH', a.attendanceDate) = :month AND " +
           "FUNCTION('YEAR', a.attendanceDate) = :year")
    List<Attendance> findByMonthAndYear(
            @Param("month") int month,
            @Param("year") int year);
}
