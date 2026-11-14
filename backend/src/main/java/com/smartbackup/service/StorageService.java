package com.smartbackup.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;

public interface StorageService {

    void upload(String key, String contentType, long contentLength, InputStream inputStream);

    void download(String key, Path destination);

    PresignedUrl generateDownloadUrl(String key);

    void delete(String key);

    record PresignedUrl(String url, Instant expiresAt) {
    }
}
