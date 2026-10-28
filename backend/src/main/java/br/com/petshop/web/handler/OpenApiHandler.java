package br.com.petshop.web.handler;

import br.com.petshop.web.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Publica a especificação OpenAPI em /openapi.yaml.
 */
public final class OpenApiHandler implements HttpHandler {

    private final Path arquivo;

    public OpenApiHandler(Path arquivo) {
        this.arquivo = arquivo.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(metodo)) {
                HttpUtils.enviarSemConteudo(exchange, 204);
                return;
            }
            if (!"GET".equals(metodo) && !"HEAD".equals(metodo)) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD, OPTIONS");
                HttpUtils.enviarErro(exchange, 405, "Método não permitido para /openapi.yaml.", null);
                return;
            }
            String caminho = exchange.getRequestURI().getPath();
            if (!"/openapi.yaml".equals(caminho)) {
                HttpUtils.enviarErro(exchange, 404, "Especificação OpenAPI não encontrada.", null);
                return;
            }
            if (!Files.isRegularFile(arquivo)) {
                HttpUtils.enviarErro(exchange, 404, "Especificação OpenAPI não encontrada.", null);
                return;
            }
            HttpUtils.enviarArquivo(exchange, arquivo, "HEAD".equals(metodo));
        } finally {
            exchange.close();
        }
    }
}
