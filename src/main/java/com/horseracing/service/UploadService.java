package com.horseracing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");

    private final Path evidenceDirectory;

    public UploadService(@Value("${app.upload.evidence-dir:uploads/evidence}") String evidenceDirectory) {
        this.evidenceDirectory = Paths.get(evidenceDirectory).toAbsolutePath().normalize();
    }

    public Map<String, Object> uploadEvidence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required.");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!isAllowedFile(contentType, extension)) {
            throw new IllegalArgumentException("Evidence file must be an image or PDF.");
        }

        try {
            Files.createDirectories(evidenceDirectory);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = evidenceDirectory.resolve(fileName).normalize();
            file.transferTo(target);
            String publicUrl = "/uploads/evidence/" + fileName;

            return Map.of(
                    "fileName", fileName,
                    "url", publicUrl,
                    "path", publicUrl,
                    "contentType", contentType == null ? "" : contentType,
                    "size", file.getSize()
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("Evidence file could not be uploaded.");
        }
    }

    public Path getEvidenceDirectory() {
        return evidenceDirectory;
    }

    private boolean isAllowedFile(String contentType, String extension) {
        boolean validContentType = contentType != null
                && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        return validContentType && ALLOWED_EXTENSIONS.contains(extension);
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Evidence file extension is required.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Evidence file must be an image or PDF.");
        }
        return extension;
    }
}
