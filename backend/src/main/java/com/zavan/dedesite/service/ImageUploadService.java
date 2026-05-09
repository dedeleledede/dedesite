package com.zavan.dedesite.service;

import java.io.IOException;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageUploadService {

    @Value("${app.upload.dir:/var/www/uploads}")
    private String uploadDir;

    public Map<String, String> uploadBlogImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo vazio");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Somente imagens");
        }

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.')))
                .orElse(".bin");

        return saveBlogImage(file.getBytes(), ext);
    }

    public Map<String, String> uploadBlogImageData(String dataUrl, String originalFilename) throws IOException {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem vazia");
        }

        int commaIndex = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:image/") || commaIndex < 0) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Somente imagens");
        }

        String metadata = dataUrl.substring(0, commaIndex);
        if (!metadata.contains(";base64")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Imagem precisa estar em base64");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base64 invalido");
        }

        String ext = Optional.ofNullable(originalFilename)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.')))
                .orElseGet(() -> extensionFromMetadata(metadata));

        return saveBlogImage(bytes, ext);
    }

    private Map<String, String> saveBlogImage(byte[] bytes, String ext) throws IOException {
        Path dir = Paths.get(uploadDir, "blog");
        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + sanitizeExtension(ext);
        Path dest = dir.resolve(filename);
        Files.write(dest, bytes);

        return Map.of("url", "/uploads/blog/" + filename);
    }

    private String sanitizeExtension(String ext) {
        String cleanExt = ext == null || ext.isBlank() ? ".bin" : ext.toLowerCase();
        if (!cleanExt.startsWith(".")) {
            cleanExt = "." + cleanExt;
        }
        return cleanExt.replaceAll("[^a-z0-9.]", "");
    }

    private String extensionFromMetadata(String metadata) {
        if (metadata.contains("image/jpeg")) return ".jpg";
        if (metadata.contains("image/png")) return ".png";
        if (metadata.contains("image/gif")) return ".gif";
        if (metadata.contains("image/webp")) return ".webp";
        if (metadata.contains("image/svg")) return ".svg";
        return ".bin";
    }
}
