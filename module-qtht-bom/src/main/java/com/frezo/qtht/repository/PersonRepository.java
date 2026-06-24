package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, String>, JpaSpecificationExecutor<Person> {
    Optional<Person> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByCode(String code);

    Optional<Person> findByIdAndIsDeletedFalse(String id);

    long countByCreatedDateAfter(LocalDateTime date);
}
