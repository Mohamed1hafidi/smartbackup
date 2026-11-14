package com.smartbackup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String publicEndpoint,
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess,
        long presignedUrlExpirySeconds
) {
}
