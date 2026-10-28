package br.com.petshop.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

public final class GoogleCloudStorageService {

    private final Storage storage;
    private final String bucketName;

    public GoogleCloudStorageService(StorageConfig config) throws IOException {
        this.bucketName = config.bucketName();
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        if (config.projectId() != null && !config.projectId().isBlank()) {
            builder.setProjectId(config.projectId());
        }
        if (config.credentialsPath() != null && Files.exists(config.credentialsPath())) {
            try (InputStream in = Files.newInputStream(config.credentialsPath())) {
                builder.setCredentials(GoogleCredentials.fromStream(in));
            }
        } else {
            builder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        this.storage = builder.build().getService();
    }

    public String upload(InputStream inputStream, String originalFileName, String contentType) throws IOException {
        String ext = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf('.'));
        }
        String objectName = UUID.randomUUID().toString() + ext;
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId);
        if (contentType != null && !contentType.isBlank()) {
            builder.setContentType(contentType);
        }
        BlobInfo blobInfo = builder.build();
        storage.createFrom(blobInfo, inputStream);
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
    }

    public boolean deleteByUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (!url.startsWith(prefix)) {
            return false;
        }
        String objectName = url.substring(prefix.length());
        if (objectName.isEmpty()) {
            return false;
        }
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            return storage.delete(blobId);
        } catch (Exception e) {
            return false;
        }
    }
}
