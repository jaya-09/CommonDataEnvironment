package com.cde.plm.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * StorageService implementation backed by MinIO (S3-compatible).
 * Active when app.storage.provider=minio (default for local/Docker dev).
 *
 * To switch to GCP Cloud Storage later:
 *   1. Add google-cloud-storage dependency to pom.xml
 *   2. Create GcpStorageService implements StorageService with @ConditionalOnProperty(havingValue="gcp")
 *   3. Set app.storage.provider=gcp in application.yml and fill in the GCP block
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Value("${app.storage.minio.endpoint}")
    private String endpoint;

    @Override
    public String upload(String objectKey, InputStream inputStream, String contentType, long sizeBytes) {
        try {
            ensureBucketExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, sizeBytes, -1)
                            .contentType(contentType)
                            .build()
            );

            // Return a direct URL — for local MinIO this is public
            // In production MinIO you'd generate a pre-signed URL instead
            String url = endpoint + "/" + bucket + "/" + objectKey;
            log.info("Uploaded {} → {}", objectKey, url);
            return url;

        } catch (MinioException e) {
            log.error("MinIO upload failed for key {}: {}", objectKey, e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading {}: {}", objectKey, e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted object {} from bucket {}", objectKey, bucket);
        } catch (Exception e) {
            log.error("Failed to delete object {}: {}", objectKey, e.getMessage());
            throw new RuntimeException("File delete failed", e);
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);
        }
    }
}
