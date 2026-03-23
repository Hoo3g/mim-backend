package com.hus.mim_backend.infrastructure.adapter.storage;

import com.hus.mim_backend.application.port.output.ObjectStorageRepository;
import com.hus.mim_backend.application.storage.model.StoredObjectResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Infrastructure adapter that bridges the storage output port to MinIO.
 */
@Component
public class MinioObjectStorageAdapter implements ObjectStorageRepository {
    private final MinioStorageService minioStorageService;

    public MinioObjectStorageAdapter(MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    @Override
    public String uploadResearchPdf(MultipartFile file) {
        return minioStorageService.uploadResearchPdf(file);
    }

    @Override
    public String uploadResearchHeroImage(MultipartFile file) {
        return minioStorageService.uploadResearchHeroImage(file);
    }

    @Override
    public String uploadProfileCv(MultipartFile file) {
        return minioStorageService.uploadProfileCv(file);
    }

    @Override
    public String uploadAvatarImage(MultipartFile file) {
        return minioStorageService.uploadAvatarImage(file);
    }

    @Override
    public Optional<StoredObjectResource> readResearchPdf(String objectKey) {
        return minioStorageService.readResearchPdf(objectKey).map(this::mapStoredObject);
    }

    @Override
    public Optional<StoredObjectResource> readResearchHeroImage(String objectKey) {
        return minioStorageService.readResearchHeroImage(objectKey).map(this::mapStoredObject);
    }

    @Override
    public Optional<StoredObjectResource> readProfileCv(String objectKey) {
        return minioStorageService.readProfileCv(objectKey).map(this::mapStoredObject);
    }

    @Override
    public Optional<StoredObjectResource> readAvatarImage(String objectKey) {
        return minioStorageService.readAvatarImage(objectKey).map(this::mapStoredObject);
    }

    private StoredObjectResource mapStoredObject(MinioStorageService.StoredObject storedObject) {
        return new StoredObjectResource(
                storedObject.stream(),
                storedObject.contentType(),
                storedObject.size(),
                storedObject.originalFilename());
    }
}
