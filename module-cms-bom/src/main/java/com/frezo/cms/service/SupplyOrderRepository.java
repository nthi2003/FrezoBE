package com.frezo.cms.service;

import com.frezo.cms.entity.SupplyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, String> {

}
