package br.com.petshop.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurações da aplicação.
 *
 * <p>Os valores são lidos de um arquivo .env (ou do caminho indicado em
 * APP_ENV_FILE) e podem ser sobrescritos por variáveis de ambiente. Assim,
 * nenhuma credencial fica escrita no código-fonte.</p>
 */
public record AppConfig(
        String dbHost,
        int dbPort,
        String dbName,
        String dbUser,
        String dbPassword,
        String serverHost,
        int serverPort
) {

    private static final List<String> CHAVES = List.of(
            "DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD",
            "SERVER_HOST", "SERVER_PORT"
    );

    public static AppConfig load() throws IOException {
        Map<String, String> valores = new HashMap<>();

        String envFile = System.getenv("APP_ENV_FILE");
        Path arquivo = Path.of(envFile == null || envFile.isBlank() ? ".env" : envFile);
        if (Files.isRegularFile(arquivo)) {
            lerArquivoEnv(arquivo, valores);
        }

        // Variáveis de ambiente têm prioridade sobre o arquivo .env.
        for (String chave : CHAVES) {
            String valor = System.getenv(chave);
            if (valor != null && !valor.isBlank()) {
                valores.put(chave, valor.trim());
            }
        }

        String senha = valores.get("DB_PASSWORD");
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                    "DB_PASSWORD não configurada. Crie o arquivo .env a partir de .env.example "
                            + "ou defina a variável de ambiente DB_PASSWORD.");
        }

        return new AppConfig(
                valorOuPadrao(valores, "DB_HOST", "localhost"),
                lerPorta(valores, "DB_PORT", 5432),
                valorOuPadrao(valores, "DB_NAME", "petshop"),
                valorOuPadrao(valores, "DB_USER", "postgres"),
                senha,
                valorOuPadrao(valores, "SERVER_HOST", "localhost"),
                lerPorta(valores, "SERVER_PORT", 8080)
        );
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    private static void lerArquivoEnv(Path arquivo, Map<String, String> destino) throws IOException {
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
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
            int separador = texto.indexOf('=');
            if (separador <= 0) {
                continue;
            }
            String chave = texto.substring(0, separador).trim();
            String valor = texto.substring(separador + 1).trim();
            if (valor.length() >= 2) {
                char primeiro = valor.charAt(0);
                char ultimo = valor.charAt(valor.length() - 1);
                if ((primeiro == '"' && ultimo == '"') || (primeiro == '\'' && ultimo == '\'')) {
                    valor = valor.substring(1, valor.length() - 1);
                }
            }
            destino.put(chave, valor);
        }
    }

    private static String valorOuPadrao(Map<String, String> valores, String chave, String padrao) {
        String valor = valores.get(chave);
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    private static int lerPorta(Map<String, String> valores, String chave, int padrao) {
        String valor = valores.get(chave);
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            int porta = Integer.parseInt(valor.trim());
            if (porta < 1 || porta > 65535) {
                throw new NumberFormatException("fora do intervalo");
            }
            return porta;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(chave + " deve ser um número entre 1 e 65535.", e);
        }
    }

    @Override
    public String toString() {
        return "AppConfig[dbHost=" + dbHost + ", dbPort=" + dbPort + ", dbName=" + dbName
                + ", dbUser=" + dbUser + ", serverHost=" + serverHost + ", serverPort=" + serverPort
                + ", dbPassword=***]";
    }
}
