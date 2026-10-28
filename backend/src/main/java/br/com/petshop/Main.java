package br.com.petshop;

import br.com.petshop.config.AppConfig;
import br.com.petshop.db.Database;
import br.com.petshop.repository.PetRepository;
import br.com.petshop.service.PetService;
import br.com.petshop.web.ApiServer;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponto de entrada da API.
 */
public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %3$s - %5$s%6$s%n");

        try {
            AppConfig config = AppConfig.load();
            Database database = new Database(config);
            database.checkConnection();

            PetService service = new PetService(new PetRepository(database));
            Path webDir = Path.of("web").toAbsolutePath().normalize();
            ApiServer api = new ApiServer(config, service, database, webDir);

            CountDownLatch encerramento = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Encerrando a API...");
                api.stop();
                encerramento.countDown();
            }, "petshop-shutdown"));

            api.start();

            LOGGER.info("========================================================");
            LOGGER.info("Petshop API iniciada");
            LOGGER.info("API:        " + api.baseUrl());
            LOGGER.info("Swagger UI: " + api.baseUrl() + "/docs/");
            LOGGER.info("OpenAPI:    " + api.baseUrl() + "/openapi.yaml");
            LOGGER.info("Banco:      " + config.jdbcUrl() + " (usuário: " + config.dbUser() + ")");
            LOGGER.info("========================================================");

            encerramento.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha ao iniciar a API: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
