package br.com.petshop.web.handler;

import br.com.petshop.web.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Locale;

/**
 * Redireciona a raiz para a documentação e responde 404 para rotas desconhecidas.
 */
public final class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(metodo)) {
                HttpUtils.enviarSemConteudo(exchange, 204);
                return;
            }

            String caminho = exchange.getRequestURI().getPath();
            if (caminho == null || caminho.isEmpty() || caminho.equals("/")) {
                if ("GET".equals(metodo) || "HEAD".equals(metodo)) {
                    HttpUtils.enviarRedirecionamento(exchange, "/docs/");
                } else {
                    exchange.getResponseHeaders().set("Allow", "GET, HEAD, OPTIONS");
                    HttpUtils.enviarErro(exchange, 405, "Método não permitido para /.", null);
                }
                return;
            }

            HttpUtils.enviarErro(exchange, 404, "Rota não encontrada.", null);
        } finally {
            exchange.close();
        }
    }
}
