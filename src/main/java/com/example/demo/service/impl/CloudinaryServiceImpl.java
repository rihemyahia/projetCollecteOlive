package com.example.demo.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(Cloudinary.class)
@Primary
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadAlertImage(String alerteId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "zitouna/alertes/" + alerteId,
                            "resource_type", "image"
                    )
            );
            Object secureUrl = res.get("secure_url");
            if (secureUrl == null) {
                throw new IllegalStateException("Cloudinary upload did not return secure_url");
            }
            return secureUrl.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload image to Cloudinary", e);
        }
    }
}

