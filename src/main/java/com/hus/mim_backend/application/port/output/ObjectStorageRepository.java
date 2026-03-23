package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.storage.model.StoredObjectResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Output port for object storage operations.
 */
public interface ObjectStorageRepository {
    String uploadResearchPdf(MultipartFile file);

    String uploadResearchHeroImage(MultipartFile file);

    String uploadProfileCv(MultipartFile file);

    String uploadAvatarImage(MultipartFile file);

    Optional<StoredObjectResource> readResearchPdf(String objectKey);

    Optional<StoredObjectResource> readResearchHeroImage(String objectKey);

    Optional<StoredObjectResource> readProfileCv(String objectKey);

    Optional<StoredObjectResource> readAvatarImage(String objectKey);
}
