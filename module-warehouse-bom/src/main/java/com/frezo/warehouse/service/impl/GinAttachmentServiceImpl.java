package com.frezo.warehouse.service.impl;

import com.frezo.common.service.MinioService;
import com.frezo.warehouse.entity.GinAttachment;
import com.frezo.warehouse.repository.GinAttachmentRepository;
import com.frezo.warehouse.service.GinAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GinAttachmentServiceImpl implements GinAttachmentService {

    private final GinAttachmentRepository attachmentRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public GinAttachment upload(String ginId, MultipartFile file, String note) {
        String objectName = "gin/" + ginId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String fileUrl = minioService.uploadFile(objectName, file);

        GinAttachment attachment = GinAttachment.builder()
                .ginId(ginId)
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
    public List<GinAttachment> listByGinId(String ginId) {
        return attachmentRepository.findByGinId(ginId);
    }

    @Override
    @Transactional
    public void delete(String attachmentId) {
        attachmentRepository.deleteById(attachmentId);
    }
}
