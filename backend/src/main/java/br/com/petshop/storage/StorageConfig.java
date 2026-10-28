package br.com.petshop.storage;

import br.com.petshop.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class StorageConfig {

    private static final String PROP_PROJECT_ID = "GCP_PROJECT_ID";
    private static final String PROP_BUCKET_NAME = "GCS_BUCKET_NAME";
    private static final String PROP_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";

    private final String projectId;
    private final String bucketName;
    private final Path credentialsPath;

    private StorageConfig(String projectId, String bucketName, Path credentialsPath) {
        this.projectId = projectId;
        this.bucketName = bucketName;
        this.credentialsPath = credentialsPath;
    }

    public static StorageConfig from(AppConfig config) throws IOException {
        Properties props = new Properties();
        Path env = Path.of(".env");
        if (Files.isRegularFile(env)) {
            for (String linha : Files.readAllLines(env, java.nio.charset.StandardCharsets.UTF_8)) {
                String texto = linha;
                if (texto.startsWith("\uFEFF")) {
                    texto = texto.substring(1);
                }
                texto = texto.trim();
                if (texto.isEmpty() || texto.startsWith("#")) {
                    continue;
                }
                if (texto.startsWith("export ")) {
                    texto = texto.substring("export ".length()).trim();
                }
                int sep = texto.indexOf('=');
                if (sep <= 0) {
                    continue;
                }
                String chave = texto.substring(0, sep).trim();
                String valor = texto.substring(sep + 1).trim();
                if (valor.length() >= 2) {
                    char primeiro = valor.charAt(0);
                    char ultimo = valor.charAt(valor.length() - 1);
                    if ((primeiro == '"' && ultimo == '"') || (primeiro == '\'' && ultimo == '\'')) {
                        valor = valor.substring(1, valor.length() - 1);
                    }
                }
                props.setProperty(chave, valor);
            }
        }
        String envProject = System.getenv(PROP_PROJECT_ID);
        String envBucket = System.getenv(PROP_BUCKET_NAME);
        String envCreds = System.getenv(PROP_CREDENTIALS);
        if (envProject != null && !envProject.isBlank()) {
            props.setProperty(PROP_PROJECT_ID, envProject.trim());
        }
        if (envBucket != null && !envBucket.isBlank()) {
            props.setProperty(PROP_BUCKET_NAME, envBucket.trim());
        }
        if (envCreds != null && !envCreds.isBlank()) {
            props.setProperty(PROP_CREDENTIALS, envCreds.trim());
        }

        String projectId = props.getProperty(PROP_PROJECT_ID);
        String bucketName = props.getProperty(PROP_BUCKET_NAME);
        String creds = props.getProperty(PROP_CREDENTIALS);

        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("GCS_BUCKET_NAME não configurado.");
        }

        Path credsPath = null;
        if (creds != null && !creds.isBlank()) {
            credsPath = Path.of(creds);
        }

        return new StorageConfig(projectId, bucketName, credsPath);
    }

    public String projectId() {
        return projectId;
    }

    public String bucketName() {
        return bucketName;
    }

    public Path credentialsPath() {
        return credentialsPath;
    }
}
