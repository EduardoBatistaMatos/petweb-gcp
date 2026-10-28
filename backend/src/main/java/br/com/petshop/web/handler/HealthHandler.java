package br.com.petshop.web.handler;

import br.com.petshop.db.Database;
import br.com.petshop.web.http.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoint de verificação de saúde da API e do banco.
 */
public final class HealthHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(HealthHandler.class.getName());

    private final Database database;

    public HealthHandler(Database database) {
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String metodo = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("OPTIONS".equals(metodo)) {
                HttpUtils.enviarSemConteudo(exchange, 204);
                return;
            }
            if (!"GET".equals(metodo)) {
                exchange.getResponseHeaders().set("Allow", "GET, OPTIONS");
                HttpUtils.enviarErro(exchange, 405, "Método não permitido para /health.", null);
                return;
            }

            String caminho = exchange.getRequestURI().getPath();
            if (!caminho.equals("/health")) {
                HttpUtils.enviarErro(exchange, 404, "Rota não encontrada.", null);
                return;
            }

            try {
                database.checkConnection();
                HttpUtils.enviarJson(exchange, 200, Map.of(
                        "status", "ok",
                        "database", "up"
                ));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Banco de dados indisponível na verificação de saúde", e);
                HttpUtils.enviarJson(exchange, 503, Map.of(
                        "status", "degraded",
                        "database", "down"
                ));
            }
        } finally {
            exchange.close();
        }
    }
}
