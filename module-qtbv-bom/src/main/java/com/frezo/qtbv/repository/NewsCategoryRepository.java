package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsCategoryRepository extends JpaRepository<NewsCategory, String> {

    List<NewsCategory> findByIsDeletedFalseOrderByOrderIndexAscNameAsc();

    List<NewsCategory> findByOrganizationIdAndIsDeletedFalseOrderByOrderIndexAscNameAsc(String organizationId);

    List<NewsCategory> findByOrganizationIdIsNullAndIsDeletedFalseOrderByOrderIndexAscNameAsc();
}
