package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Menu;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, String> {
    List<Menu> findByIsDeletedFalse();

    Optional<Menu> findByIdAndIsDeletedFalse(String id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    List<Menu> findByAppCode(String appCode);

    Optional<Menu> findByCodeAndAppCode(String code, String appCode);

    List<Menu> findByCodeIn(Collection<String> codes);
}
