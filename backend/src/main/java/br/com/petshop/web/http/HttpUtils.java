package br.com.petshop.web.http;

import br.com.petshop.web.json.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Utilitários de HTTP e JSON compartilhados pelos handlers.
 */
public final class HttpUtils {

    private static final int TAMANHO_MAXIMO_CORPO = 1_048_576; // 1 MiB

    private HttpUtils() {
    }

    public static <T> T lerJson(HttpExchange exchange, Class<T> tipo) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        if (!mediaType.equalsIgnoreCase("application/json")) {
            throw ApiException.unsupportedMediaType(
                    "Content-Type deve ser application/json.");
        }

        byte[] corpo;
        try (InputStream entrada = exchange.getRequestBody()) {
            corpo = entrada.readNBytes(TAMANHO_MAXIMO_CORPO + 1);
        }
        if (corpo.length > TAMANHO_MAXIMO_CORPO) {
            throw ApiException.payloadTooLarge(
                    "O corpo da requisição excede o limite de 1 MiB.");
        }
        if (corpo.length == 0) {
            throw ApiException.badRequest("O corpo da requisição é obrigatório.");
        }

        return Json.MAPPER.readValue(corpo, tipo);
    }

    public static byte[] lerCorpo(HttpExchange exchange, int limite) throws IOException {
        try (InputStream entrada = exchange.getRequestBody()) {
            return entrada.readNBytes(limite + 1);
        }
    }

    public static String contentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType == null ? "" : contentType.trim();
    }

    public static String getHeader(HttpExchange exchange, String nome) {
        if (nome == null) {
            return null;
        }
        String valor = exchange.getRequestHeaders().getFirst(nome);
        if (valor != null) {
            return valor;
        }
        valor = exchange.getRequestHeaders().getFirst(nome.toLowerCase(Locale.ROOT));
        if (valor != null) {
            return valor;
        }
        for (java.util.Map.Entry<String, java.util.List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(nome)) {
                java.util.List<String> lista = entry.getValue();
                if (lista != null && !lista.isEmpty()) {
                    return lista.get(0);
                }
            }
        }
        return null;
    }

    public static void enviarJson(HttpExchange exchange, int status, Object corpo) throws IOException {
        byte[] bytes = Json.MAPPER.writeValueAsBytes(corpo);
        cabecalhosComuns(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    public static void enviarSemConteudo(HttpExchange exchange, int status) throws IOException {
        cabecalhosComuns(exchange);
        exchange.sendResponseHeaders(status, -1);
    }

    public static void enviarErro(
            HttpExchange exchange, int status, String mensagem, List<String> detalhes)
            throws IOException {
        ErrorResponse erro = new ErrorResponse(
                OffsetDateTime.now().toString(),
                status,
                motivo(status),
                mensagem,
                exchange.getRequestURI().getPath(),
                detalhes == null ? List.of() : detalhes
        );
        enviarJson(exchange, status, erro);
    }

    public static void enviarRedirecionamento(HttpExchange exchange, String local) throws IOException {
        cabecalhosComuns(exchange);
        exchange.getResponseHeaders().set("Location", local);
        exchange.sendResponseHeaders(302, -1);
    }

    public static void enviarArquivo(HttpExchange exchange, Path arquivo, boolean head)
            throws IOException {
        long tamanho = Files.size(arquivo);
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", tipoConteudo(arquivo));
        headers.set("Cache-Control", "no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, head ? -1 : tamanho);
        if (!head) {
            try (OutputStream saida = exchange.getResponseBody()) {
                Files.copy(arquivo, saida);
            }
        }
    }

    private static void cabecalhosComuns(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Access-Control-Expose-Headers", "Location");
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
    }

    private static String tipoConteudo(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nome.endsWith(".html") || nome.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        }
        if (nome.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (nome.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (nome.endsWith(".yaml") || nome.endsWith(".yml")) {
            return "application/yaml; charset=UTF-8";
        }
        if (nome.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (nome.endsWith(".png")) {
            return "image/png";
        }
        if (nome.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static String motivo(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 413 -> "Payload Too Large";
            case 415 -> "Unsupported Media Type";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }

    public record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String path,
            List<String> details
    ) {
    }
}
