package org.noteplus.noteplus.service.impl;

import jakarta.annotation.PostConstruct;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename: " + originalFilename);
        }
        String storagePath = UUID.randomUUID() + "_" + originalFilename;
        try {
            Files.copy(file.getInputStream(), rootLocation.resolve(storagePath),
                    StandardCopyOption.REPLACE_EXISTING);
            return storagePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storagePath, e);
        }
    }

    @Override
    public Resource load(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File not found: " + storagePath);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + storagePath);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + storagePath, e);
        }
    }
}
