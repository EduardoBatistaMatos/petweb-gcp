package br.com.petshop.web.handler;

import br.com.petshop.web.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Serve os arquivos estáticos do Swagger UI em /docs/.
 */
public final class DocsHandler implements HttpHandler {

    private static final String PREFIXO = "/docs";

    private final Path diretorio;

    public DocsHandler(Path diretorio) {
        this.diretorio = diretorio.toAbsolutePath().normalize();
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
                HttpUtils.enviarErro(exchange, 405, "Método não permitido para /docs.", null);
                return;
            }

            String caminho = exchange.getRequestURI().getPath();
            if (!caminho.equals(PREFIXO) && !caminho.startsWith(PREFIXO + "/")) {
                HttpUtils.enviarErro(exchange, 404, "Rota não encontrada.", null);
                return;
            }
            if (caminho.equals(PREFIXO)) {
                HttpUtils.enviarRedirecionamento(exchange, PREFIXO + "/");
                return;
            }

            String relativo = caminho.substring(PREFIXO.length());
            if (relativo.equals("/") || relativo.isEmpty()) {
                relativo = "/index.html";
            }
            if (relativo.startsWith("/")) {
                relativo = relativo.substring(1);
            }
            relativo = URLDecoder.decode(relativo, StandardCharsets.UTF_8);

            Path arquivo = diretorio.resolve(relativo).normalize();
            if (!arquivo.startsWith(diretorio) || !Files.isRegularFile(arquivo)) {
                HttpUtils.enviarErro(exchange, 404, "Arquivo de documentação não encontrado.", null);
                return;
            }

            HttpUtils.enviarArquivo(exchange, arquivo, "HEAD".equals(metodo));
        } catch (IllegalArgumentException e) {
            HttpUtils.enviarErro(exchange, 400, "Caminho de documentação inválido.", null);
        } finally {
            exchange.close();
        }
    }
}
