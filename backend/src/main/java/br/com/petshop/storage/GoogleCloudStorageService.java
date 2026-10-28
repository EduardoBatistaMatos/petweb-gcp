package br.com.petshop.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GoogleCloudStorageService {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudStorageService.class.getName());

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
        try {
            String objectName = extrairNomeObjeto(url);
            if (objectName == null || objectName.isBlank()) return false;
            BlobId blobId = BlobId.of(bucketName, objectName);
            // Tenta excluir diretamente, sem exigir permissão de leitura prévia.
            // O retorno false exige confirmação de ausência antes de limpar o banco.
            boolean removido = storage.delete(blobId);
            if (removido) return true;
            boolean aindaExiste = storage.get(blobId) != null;
            if (aindaExiste) {
                LOGGER.warning("O GCS não removeu o objeto " + objectName + " do bucket " + bucketName);
            }
            return !aindaExiste;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha ao excluir objeto do bucket " + bucketName + " usando a URL " + url, e);
            return false;
        }
    }

    private String extrairNomeObjeto(String valor) {
        try {
            URI uri = URI.create(valor);
            String caminho = uri.getRawPath();
            String codificado;
            if ("gs".equalsIgnoreCase(uri.getScheme())) {
                if (!bucketName.equals(uri.getHost())) return null;
                codificado = caminho == null ? "" : caminho.replaceFirst("^/", "");
            } else if ("storage.googleapis.com".equalsIgnoreCase(uri.getHost())) {
                String prefixo = "/" + bucketName + "/";
                if (caminho == null || !caminho.startsWith(prefixo)) return null;
                codificado = caminho.substring(prefixo.length());
            } else if ((bucketName + ".storage.googleapis.com").equalsIgnoreCase(uri.getHost())) {
                codificado = caminho == null ? "" : caminho.replaceFirst("^/", "");
            } else {
                return null;
            }
            return URLDecoder.decode(codificado, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
