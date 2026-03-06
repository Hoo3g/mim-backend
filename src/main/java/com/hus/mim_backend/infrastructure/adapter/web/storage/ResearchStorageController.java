package com.hus.mim_backend.infrastructure.adapter.web.storage;

import com.hus.mim_backend.infrastructure.adapter.storage.MinioStorageService;
import com.hus.mim_backend.infrastructure.adapter.web.storage.dto.ResearchPdfUploadResponse;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Handles upload and retrieval of research PDFs through MinIO.
 */
@RestController
public class ResearchStorageController {
    private final MinioStorageService storageService;

    public ResearchStorageController(MinioStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(path = ApiEndpoints.STORAGE + ApiEndpoints.RESEARCH_PDFS,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResearchPdfUploadResponse>> uploadResearchPdf(
            @RequestPart("file") MultipartFile file) {
        String objectKey = storageService.uploadResearchPdf(file);
        String fileUrl = buildPublicFileUrl(objectKey);

        ResearchPdfUploadResponse response = new ResearchPdfUploadResponse(objectKey, fileUrl);
        return ResponseEntity.ok(ApiResponse.success(response, "Research PDF uploaded"));
    }

    @GetMapping(path = ApiEndpoints.PUBLIC_STORAGE + ApiEndpoints.RESEARCH_PDFS + "/{objectKey:.+}")
    public ResponseEntity<InputStreamResource> getResearchPdf(@PathVariable String objectKey) {
        Optional<MinioStorageService.StoredObject> objectOpt = storageService.readResearchPdf(objectKey);
        if (objectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MinioStorageService.StoredObject object = objectOpt.get();
        MediaType mediaType = MediaType.APPLICATION_PDF;
        if (StringUtils.hasText(object.contentType())) {
            mediaType = MediaType.parseMediaType(object.contentType());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(objectKey).build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .contentLength(object.size())
                .body(new InputStreamResource(object.stream()));
    }

    private String buildPublicFileUrl(String objectKey) {
        String encodedKey = UriUtils.encodePathSegment(objectKey, StandardCharsets.UTF_8);
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiEndpoints.PUBLIC_STORAGE + ApiEndpoints.RESEARCH_PDFS + "/" + encodedKey)
                .toUriString();
    }
}
