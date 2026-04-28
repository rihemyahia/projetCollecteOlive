package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadAlertImage(String alerteId, MultipartFile file);
}

