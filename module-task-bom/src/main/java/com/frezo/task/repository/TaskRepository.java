package com.frezo.task.repository;

import com.frezo.task.common.TaskStatusEnum;
import com.frezo.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    Optional<Task> findByIdAndIsDeletedFalse(String id);

    long countByStatusNotAndIsDeletedFalse(TaskStatusEnum status);

    /** Pending = còn mở / đang làm / chờ duyệt (không gồm CLOSED/CANCELLED). */
    long countByStatusInAndIsDeletedFalse(Collection<TaskStatusEnum> statuses);
}
