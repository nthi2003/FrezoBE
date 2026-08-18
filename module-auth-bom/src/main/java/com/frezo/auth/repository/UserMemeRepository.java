package com.frezo.auth.repository;

import com.frezo.auth.entity.UserMeme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMemeRepository extends JpaRepository<UserMeme, String> {

    List<UserMeme> findByUsernameAndIsDeletedFalseOrderByCreatedDateDesc(String username);

    long countByUsernameAndIsDeletedFalse(String username);
}
