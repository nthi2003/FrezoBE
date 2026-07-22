package com.frezo.product.service.impl.product;

import com.frezo.common.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Upload ảnh sản phẩm lên MinIO — tách riêng để façade không cần biết detail bucket.
 */
@Component
@RequiredArgsConstructor
public class ProductImageService {

    private static final String BUCKET = "freo-prod";
    private static final String PREFIX = "products/";

    private final MinioService minioService;

    public Map<String, Object> uploadImage(MultipartFile file) {
        String objectName = PREFIX + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String url = minioService.uploadFile(objectName, file, BUCKET);
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("fileName", file.getOriginalFilename());
        return result;
    }
}
