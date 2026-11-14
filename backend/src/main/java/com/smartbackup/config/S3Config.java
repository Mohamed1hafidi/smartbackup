package com.smartbackup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(region(properties)))
                .credentialsProvider(credentials(properties))
                .endpointOverride(URI.create(endpoint(properties)))
                .forcePathStyle(properties.pathStyleAccess())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        return S3Presigner.builder()
                .region(Region.of(region(properties)))
                .credentialsProvider(credentials(properties))
                .endpointOverride(URI.create(publicEndpoint(properties)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build())
                .build();
    }

    private AwsCredentialsProvider credentials(StorageProperties properties) {
        if (isBlank(properties.accessKey()) || isBlank(properties.secretKey())) {
            throw new IllegalStateException(
                    "R2_ACCESS_KEY_ID et R2_SECRET_ACCESS_KEY doivent être configurés");
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }

    private String endpoint(StorageProperties properties) {
        if (isBlank(properties.endpoint())) {
            throw new IllegalStateException("R2_ENDPOINT doit être configuré");
        }
        return properties.endpoint();
    }

    private String publicEndpoint(StorageProperties properties) {
        return isBlank(properties.publicEndpoint()) ? endpoint(properties) : properties.publicEndpoint();
    }

    private String region(StorageProperties properties) {
        return isBlank(properties.region()) ? "auto" : properties.region();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
