package com.hus.mim_backend.application.storage.service;

import com.hus.mim_backend.application.port.output.ObjectStorageRepository;
import com.hus.mim_backend.application.storage.model.StoredObjectResource;
import com.hus.mim_backend.application.storage.usecase.StorageUseCase;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Application service that delegates storage operations to output ports.
 */
public class StorageServiceImpl implements StorageUseCase {
    private final ObjectStorageRepository objectStorageRepository;

    public StorageServiceImpl(ObjectStorageRepository objectStorageRepository) {
        this.objectStorageRepository = objectStorageRepository;
    }

    @Override
    public String uploadResearchPdf(MultipartFile file) {
        return objectStorageRepository.uploadResearchPdf(file);
    }

    @Override
    public String uploadResearchHeroImage(MultipartFile file) {
        return objectStorageRepository.uploadResearchHeroImage(file);
    }

    @Override
    public String uploadProfileCv(MultipartFile file) {
        return objectStorageRepository.uploadProfileCv(file);
    }

    @Override
    public String uploadAvatarImage(MultipartFile file) {
        return objectStorageRepository.uploadAvatarImage(file);
    }

    @Override
    public Optional<StoredObjectResource> readResearchPdf(String objectKey) {
        return objectStorageRepository.readResearchPdf(objectKey);
    }

    @Override
    public Optional<StoredObjectResource> readResearchHeroImage(String objectKey) {
        return objectStorageRepository.readResearchHeroImage(objectKey);
    }

    @Override
    public Optional<StoredObjectResource> readProfileCv(String objectKey) {
        return objectStorageRepository.readProfileCv(objectKey);
    }

    @Override
    public Optional<StoredObjectResource> readAvatarImage(String objectKey) {
        return objectStorageRepository.readAvatarImage(objectKey);
    }
}
