package com.hus.mim_backend.application.storage.usecase;

import com.hus.mim_backend.application.storage.model.StoredObjectResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Use case for uploading and reading binary objects.
 */
public interface StorageUseCase {
    String uploadResearchPdf(MultipartFile file);

    String uploadResearchHeroImage(MultipartFile file);

    String uploadProfileCv(MultipartFile file);

    String uploadAvatarImage(MultipartFile file);

    Optional<StoredObjectResource> readResearchPdf(String objectKey);

    Optional<StoredObjectResource> readResearchHeroImage(String objectKey);

    Optional<StoredObjectResource> readProfileCv(String objectKey);

    Optional<StoredObjectResource> readAvatarImage(String objectKey);
}
