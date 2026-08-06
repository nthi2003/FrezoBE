package com.frezo.qtbv.repository;

import com.frezo.qtbv.entity.NewsMotto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsMottoRepository extends JpaRepository<NewsMotto, String> {

    List<NewsMotto> findByIsDeletedFalseOrderByCreatedDateDesc();
}
