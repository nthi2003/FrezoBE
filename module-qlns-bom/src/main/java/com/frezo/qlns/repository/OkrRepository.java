package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Okr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OkrRepository extends JpaRepository<Okr, String> {
    List<Okr> findByIsDeletedFalseOrderByCreatedDateDesc();
    List<Okr> findByOwnerPersonIdAndIsDeletedFalse(String ownerPersonId);
    List<Okr> findByOwnerPersonIdInAndIsDeletedFalseOrderByCreatedDateDesc(Collection<String> ownerPersonIds);
}
