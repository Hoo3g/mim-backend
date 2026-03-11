package com.hus.mim_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application storage settings for MinIO.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.minio")
public class MinioStorageProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

}

