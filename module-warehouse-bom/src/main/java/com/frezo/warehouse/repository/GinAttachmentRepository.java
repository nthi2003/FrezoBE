package com.frezo.warehouse.repository;

import com.frezo.warehouse.entity.GinAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GinAttachmentRepository extends JpaRepository<GinAttachment, String> {
    List<GinAttachment> findByGinId(String ginId);
    void deleteByGinId(String ginId);
}
