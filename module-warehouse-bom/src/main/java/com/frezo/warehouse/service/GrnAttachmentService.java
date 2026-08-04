package com.frezo.warehouse.service;

import com.frezo.warehouse.entity.GrnAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GrnAttachmentService {

    GrnAttachment upload(String grnId, MultipartFile file, String note);

    List<GrnAttachment> listByGrnId(String grnId);

    void delete(String attachmentId);
}
