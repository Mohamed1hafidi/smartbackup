package com.smartbackup.service;

import com.smartbackup.config.StorageProperties;
import com.smartbackup.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class R2StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @Override
    public void upload(String key, String contentType, long contentLength, InputStream inputStream) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (SdkException e) {
            throw new StorageException("Erreur lors de l'upload vers le stockage", e);
        }
    }

    @Override
    public void download(String key, Path destination) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build();

        try (OutputStream outputStream = Files.newOutputStream(destination)) {
            s3Client.getObject(request, ResponseTransformer.toOutputStream(outputStream));
        } catch (IOException e) {
            throw new StorageException("Erreur lors de l'écriture du fichier téléchargé", e);
        } catch (SdkException e) {
            throw new StorageException("Erreur lors du téléchargement depuis le stockage", e);
        }
    }

    @Override
    public PresignedUrl generateDownloadUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build();

        Duration signatureDuration = Duration.ofSeconds(storageProperties.presignedUrlExpirySeconds());
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            return new PresignedUrl(url, Instant.now().plus(signatureDuration));
        } catch (SdkException e) {
            throw new StorageException("Erreur lors de la génération de l'URL signée", e);
        }
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new StorageException("Erreur lors de la suppression du stockage", e);
        }
    }
}
