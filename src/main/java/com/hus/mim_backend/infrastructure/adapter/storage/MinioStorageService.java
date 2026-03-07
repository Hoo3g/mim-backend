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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter service for storing and reading research PDFs in MinIO.
 */
@Service
public class MinioStorageService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

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

        String originalFilename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean pdfMime = PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType());
        boolean pdfExtension = originalFilename.endsWith(".pdf");
        if (!pdfMime && !pdfExtension) {
            throw new DomainException("Only PDF files are allowed");
        }

        String objectKey = buildPdfObjectKey();
        return uploadObject(file, objectKey, PDF_CONTENT_TYPE, "Failed to upload PDF to MinIO");
    }

    public String uploadResearchHeroImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("Image file is required");
        }

        String originalFilename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extractExtension(originalFilename);

        boolean imageMime = IMAGE_CONTENT_TYPES.contains(contentType);
        boolean imageExtension = IMAGE_EXTENSIONS.contains(extension);
        if (!imageMime && !imageExtension) {
            throw new DomainException("Only JPG, PNG, WEBP images are allowed");
        }

        String normalizedContentType = normalizeImageContentType(contentType, extension);
        String normalizedExtension = normalizeImageExtension(extension, normalizedContentType);
        String objectKey = "research-hero-" + UUID.randomUUID() + "." + normalizedExtension;
        return uploadObject(file, objectKey, normalizedContentType, "Failed to upload image to MinIO");
    }

    public Optional<StoredObject> readResearchPdf(String objectKey) {
        return readObject(objectKey, PDF_CONTENT_TYPE, "Failed to read PDF from MinIO");
    }

    public Optional<StoredObject> readResearchHeroImage(String objectKey) {
        return readObject(objectKey, "image/jpeg", "Failed to read image from MinIO");
    }

    private Optional<StoredObject> readObject(String objectKey, String fallbackContentType, String errorMessage) {
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
                contentType = fallbackContentType;
            }

            return Optional.of(new StoredObject(stream, contentType, stat.size()));
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())
                    || "NoSuchBucket".equalsIgnoreCase(ex.errorResponse().code())) {
                return Optional.empty();
            }
            throw new IllegalStateException(errorMessage, ex);
        } catch (Exception ex) {
            throw new IllegalStateException(errorMessage, ex);
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

    private String buildPdfObjectKey() {
        return "research-paper-" + UUID.randomUUID() + ".pdf";
    }

    private String uploadObject(MultipartFile file, String objectKey, String contentType, String errorMessage) {
        try {
            ensureBucketExists();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getBucket())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(contentType)
                                .build());
            }
            return objectKey;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }

    private String normalizeImageContentType(String mime, String extension) {
        if (IMAGE_CONTENT_TYPES.contains(mime)) {
            return mime;
        }
        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "image/jpeg";
        };
    }

    private String normalizeImageExtension(String extension, String contentType) {
        if ("png".equals(extension) || "webp".equals(extension) || "jpg".equals(extension)) {
            return extension;
        }
        if ("jpeg".equals(extension)) {
            return "jpg";
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    public record StoredObject(InputStream stream, String contentType, long size) {
    }
}
