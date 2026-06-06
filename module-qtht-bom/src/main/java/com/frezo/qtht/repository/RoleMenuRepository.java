package com.frezo.qtht.repository;

import com.frezo.qtht.entity.Menu;
import com.frezo.qtht.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleMenuRepository extends JpaRepository<RoleMenu, String> {

  @Query("""
          select distinct m
          from RoleMenu rm
          join rm.role r
          join rm.menu m
          where r.code in :roleCodes
              and m.isDeleted = false
              and rm.isDeleted = false
          order by m.orderIndex asc, m.code asc
      """)
  List<Menu> findMenusByRoleCodes(@Param("roleCodes") List<String> roleCodes);

  List<RoleMenu> findByRole_Code(String roleCode);

  List<RoleMenu> findByRole_IdIn(List<String> roleIds);

}
