package com.agrosentinel.agrosentinel.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FailureEvidenceStorageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path storageLocation;

    public FailureEvidenceStorageService(@Value("${agrosentinel.failure-evidence.location}") String location) {
        this.storageLocation = Path.of(location).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La evidencia no puede superar 10 MB");
        }

        String extension = IMAGE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta una imagen JPG, PNG o WEBP");
        }

        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(storageLocation);
            try (var input = file.getInputStream();
                    var output = Files.newOutputStream(storageLocation.resolve(filename), StandardOpenOption.CREATE_NEW)) {
                input.transferTo(output);
            }
            return "/uploads/failure-evidence/" + filename;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la evidencia", exception);
        }
    }
}
