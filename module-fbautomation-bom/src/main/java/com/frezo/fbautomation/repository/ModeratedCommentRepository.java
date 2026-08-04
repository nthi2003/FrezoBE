package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.ModeratedComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeratedCommentRepository extends JpaRepository<ModeratedComment, String> {
    List<ModeratedComment> findByStatus(String status);
    long countByStatus(String status);
}
