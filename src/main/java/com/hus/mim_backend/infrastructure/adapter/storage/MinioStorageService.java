package com.hus.mim_backend.infrastructure.adapter.storage;

import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.infrastructure.config.MinioStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter service for storing and reading research PDFs in MinIO.
 */
@Service
public class MinioStorageService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    public MinioStorageService(MinioStorageProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    public String uploadResearchPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("PDF file is required");
        }

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean pdfMime = PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType());
        boolean pdfExtension = originalFilename.endsWith(".pdf");
        if (!pdfMime && !pdfExtension) {
            throw new DomainException("Only PDF files are allowed");
        }

        try {
            ensureBucketExists();
            String objectKey = buildObjectKey();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(PDF_CONTENT_TYPE)
                                .build());
            }
            return objectKey;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload PDF to MinIO", ex);
        }
    }

    public Optional<StoredObject> readResearchPdf(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build());

            GetObjectResponse stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build());

            String contentType = stat.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = PDF_CONTENT_TYPE;
            }

            return Optional.of(new StoredObject(stream, contentType, stat.size()));
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())
                    || "NoSuchBucket".equalsIgnoreCase(ex.errorResponse().code())) {
                return Optional.empty();
            }
            throw new IllegalStateException("Failed to read PDF from MinIO", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read PDF from MinIO", ex);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(properties.getBucket())
                            .build());
        }
    }

    private String buildObjectKey() {
        return "research-paper-" + UUID.randomUUID() + ".pdf";
    }

    public record StoredObject(InputStream stream, String contentType, long size) {
    }
}

