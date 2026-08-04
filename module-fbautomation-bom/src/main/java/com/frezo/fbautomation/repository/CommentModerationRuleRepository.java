package com.frezo.fbautomation.repository;

import com.frezo.fbautomation.entity.CommentModerationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentModerationRuleRepository extends JpaRepository<CommentModerationRule, String> {
    List<CommentModerationRule> findByEnabledTrue();
}
