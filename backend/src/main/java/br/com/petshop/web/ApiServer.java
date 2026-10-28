package br.com.petshop.web;

import br.com.petshop.config.AppConfig;
import br.com.petshop.db.Database;
import br.com.petshop.service.PetService;
import br.com.petshop.repository.PetFotoRepository;
import br.com.petshop.web.handler.PetFotoHandler;
import br.com.petshop.web.handler.DocsHandler;
import br.com.petshop.web.handler.HealthHandler;
import br.com.petshop.web.handler.OpenApiHandler;
import br.com.petshop.web.handler.PetHandler;
import br.com.petshop.web.handler.RootHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configura e inicia o servidor HTTP embutido do JDK.
 */
public final class ApiServer {

    private final HttpServer server;
    private final ExecutorService executor;
    private final AppConfig config;

    public ApiServer(AppConfig config, PetService petService, Database database, Path webDir)
            throws IOException {
        this.config = config;
        this.server = HttpServer.create(
                new InetSocketAddress(config.serverHost(), config.serverPort()), 0);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.server.setExecutor(executor);

        PetFotoHandler petFotoHandler = new PetFotoHandler(new PetFotoRepository(database), petService);
        server.createContext("/pets", new PetHandler(petService, petFotoHandler));
        server.createContext("/health", new HealthHandler(database));
        server.createContext("/docs", new DocsHandler(webDir.resolve("docs")));
        server.createContext("/openapi.yaml", new OpenApiHandler(webDir.resolve("openapi.yaml")));
        server.createContext("/", new RootHandler());
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
        executor.shutdown();
    }

    public String baseUrl() {
        String host = config.serverHost();
        if (host.equals("0.0.0.0") || host.equals("::") || host.isBlank()) {
            host = "localhost";
        }
        return "http://" + host + ":" + config.serverPort();
    }
}
