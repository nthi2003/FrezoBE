package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GrnAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrnAttachmentRepository extends JpaRepository<GrnAttachment, String> {
    List<GrnAttachment> findByGrnId(String grnId);
    void deleteByGrnId(String grnId);
}
