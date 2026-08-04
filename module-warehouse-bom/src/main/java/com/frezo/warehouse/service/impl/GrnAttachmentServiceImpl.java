package com.frezo.warehouse.service.impl;

import com.frezo.common.service.MinioService;
import com.frezo.warehouse.entity.GrnAttachment;
import com.frezo.warehouse.repository.GrnAttachmentRepository;
import com.frezo.warehouse.service.GrnAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrnAttachmentServiceImpl implements GrnAttachmentService {

    private final GrnAttachmentRepository attachmentRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public GrnAttachment upload(String grnId, MultipartFile file, String note) {
        String objectName = "grn/" + grnId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = minioService.uploadFile(objectName, file);

        GrnAttachment attachment = GrnAttachment.builder()
                .grnId(grnId)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .note(note)
                .build();
        return attachmentRepository.save(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GrnAttachment> listByGrnId(String grnId) {
        return attachmentRepository.findByGrnId(grnId);
    }

    @Override
    @Transactional
    public void delete(String attachmentId) {
        attachmentRepository.deleteById(attachmentId);
    }
}
