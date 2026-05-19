package com.myplans.core.storage;

import com.myplans.core.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(
            @Value("${storage.s3.bucket}") String bucket,
            @Value("${storage.s3.region:us-east-1}") String region) {
        this.bucket = bucket;
        // En EC2 con IAM Role las credenciales se toman automáticamente del metadata de la instancia.
        // Para desarrollo local se usan las variables AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY.
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
        log.info("S3StorageService inicializado — bucket: {}, region: {}", bucket, region);
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }
        String extension = extractExtension(file.getOriginalFilename());
        String key = folder + "/" + UUID.randomUUID() + extension;
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
            log.info("Archivo subido a S3: s3://{}/{} ({} bytes)", bucket, key, file.getSize());
            return key;
        } catch (IOException | S3Exception e) {
            throw new BusinessException("Error al subir archivo a S3: " + e.getMessage());
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return response.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException("Archivo no encontrado en S3: " + key);
        } catch (S3Exception e) {
            throw new BusinessException("Error al descargar archivo de S3: " + key);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("Archivo eliminado de S3: s3://{}/{}", bucket, key);
        } catch (S3Exception e) {
            log.warn("No se pudo eliminar de S3 {}: {}", key, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx).toLowerCase() : "";
    }
}
