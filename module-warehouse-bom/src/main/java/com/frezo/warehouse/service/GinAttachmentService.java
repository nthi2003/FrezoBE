package com.frezo.warehouse.service;

import com.frezo.warehouse.entity.GinAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GinAttachmentService {

    GinAttachment upload(String ginId, MultipartFile file, String note);

    List<GinAttachment> listByGinId(String ginId);

    void delete(String attachmentId);
}
