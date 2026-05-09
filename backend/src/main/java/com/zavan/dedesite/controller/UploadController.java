package com.zavan.dedesite.controller;

import com.zavan.dedesite.service.ImageUploadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
public class UploadController {

    private final ImageUploadService imageUploadService;

    public UploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping({
            "/api/uploads/image",
            "/blog/uploads/image",
            "/uploads/image"
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return imageUploadService.uploadBlogImage(file);
    }

    @PostMapping({
            "/api/uploads/image-data",
            "/blog/uploads/image-data",
            "/uploads/image-data"
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
    public Map<String, String> uploadDataUrl(@RequestBody Map<String, String> payload) throws IOException {
        return imageUploadService.uploadBlogImageData(payload.get("dataUrl"), payload.get("filename"));
    }

    @GetMapping("/blog/uploads/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUTHOR')")
    public Map<String, String> checkUploadRoute() {
        return Map.of(
                "status", "ok",
                "postUrl", "/blog/uploads/image"
        );
    }
}
