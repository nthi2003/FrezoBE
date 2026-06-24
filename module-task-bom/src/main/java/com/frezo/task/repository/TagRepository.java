package com.frezo.task.repository;

import com.frezo.task.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, String>  {
    boolean existsByCode (String code);

    Optional<Tag> findByIdAndIsDeletedFalse(String id);

    List<Tag> findByCategoryAndIsDeletedFalse(String category);
}
