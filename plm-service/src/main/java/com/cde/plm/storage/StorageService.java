package com.cde.plm.storage;

import java.io.InputStream;

/**
 * Abstraction over object storage providers (MinIO for local dev, GCP Cloud Storage for production).
 * Switch provider by changing app.storage.provider in application.yml.
 */
public interface StorageService {

    /**
     * Upload a file and return the URL at which it can be retrieved.
     *
     * @param objectKey   path/name inside the bucket (e.g. "versions/{versionId}/drawing.pdf")
     * @param inputStream raw file bytes
     * @param contentType MIME type of the file
     * @param sizeBytes   total byte length (-1 if unknown)
     * @return public or pre-signed URL to the stored object
     */
    String upload(String objectKey, InputStream inputStream, String contentType, long sizeBytes);

    /**
     * Delete an object from storage (used when a TDP record is removed).
     *
     * @param objectKey same key used during upload
     */
    void delete(String objectKey);
}
