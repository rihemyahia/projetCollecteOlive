package com.example.demo.service.impl;

import com.example.demo.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryServiceDisabled implements CloudinaryService {
    @Override
    public String uploadAlertImage(String alerteId, MultipartFile file) {
        throw new IllegalStateException("Cloudinary is not configured. Please set CLOUDINARY_URL env var.");
    }
}

