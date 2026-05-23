package com.cde.plm.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    /**
     * MinIO client bean — only created when app.storage.provider=minio (the default for local dev).
     * When you switch to GCP, set provider=gcp in application.yml and this bean will be skipped.
     */
    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient(
            @Value("${app.storage.minio.endpoint}") String endpoint,
            @Value("${app.storage.minio.access-key}") String accessKey,
            @Value("${app.storage.minio.secret-key}") String secretKey) {

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
