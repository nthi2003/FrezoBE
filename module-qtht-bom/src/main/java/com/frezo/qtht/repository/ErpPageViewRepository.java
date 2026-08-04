package com.frezo.qtht.repository;

import com.frezo.qtht.entity.ErpPageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ErpPageViewRepository extends JpaRepository<ErpPageView, String> {

    long countByViewedAtGreaterThanEqual(LocalDateTime from);

    @Query(value = """
            SELECT module_code AS code, COUNT(*) AS cnt
            FROM erp_page_view
            WHERE viewed_at >= :from AND module_code IS NOT NULL AND module_code <> ''
            GROUP BY module_code
            ORDER BY cnt DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topModulesSince(@Param("from") LocalDateTime from);

    @Query(value = """
            SELECT route AS route, COUNT(*) AS cnt
            FROM erp_page_view
            WHERE viewed_at >= :from
            GROUP BY route
            ORDER BY cnt DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topRoutesSince(@Param("from") LocalDateTime from);
}
