package com.myplans.core.storage;

import com.myplans.core.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${storage.base-dir:uploads}")
    private String baseDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(baseDir));
            log.info("Storage local inicializado en: {}", Paths.get(baseDir).toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de storage: " + baseDir, e);
        }
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }
        try {
            Path folderPath = Paths.get(baseDir, folder);
            Files.createDirectories(folderPath);

            String extension = extractExtension(file.getOriginalFilename());
            String key = folder + "/" + UUID.randomUUID() + extension;
            Path targetPath = Paths.get(baseDir).resolve(key);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archivo guardado: {} ({} bytes)", key, file.getSize());
            return key;
        } catch (IOException e) {
            throw new BusinessException("Error al guardar el archivo: " + e.getMessage());
        }
    }

    @Override
    public byte[] download(String key) {
        Path target = Paths.get(baseDir).resolve(key).normalize();
        if (!target.startsWith(Paths.get(baseDir).toAbsolutePath().normalize())
                && !target.startsWith(Paths.get(baseDir).normalize())) {
            throw new BusinessException("Ruta de archivo inválida");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path target = Paths.get(baseDir).resolve(key);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("No se pudo eliminar {}: {}", key, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx).toLowerCase() : "";
    }
}