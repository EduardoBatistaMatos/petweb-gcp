package br.com.petshop.db;

import br.com.petshop.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexões JDBC com o PostgreSQL.
 *
 * <p>Para manter o projeto simples, uma conexão é aberta por operação e
 * fechada pelo try-with-resources. O driver é carregado explicitamente para
 * que o erro apareça na inicialização, e não apenas na primeira requisição.</p>
 */
public final class Database {

    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Driver JDBC do PostgreSQL não encontrado. Verifique a pasta lib/.", e);
        }
    }

    public Connection getConnection() throws SQLException {
        Properties propriedades = new Properties();
        propriedades.setProperty("user", config.dbUser());
        propriedades.setProperty("password", config.dbPassword());
        propriedades.setProperty("connectTimeout", "5");
        propriedades.setProperty("socketTimeout", "30");
        propriedades.setProperty("ApplicationName", "petshop-api");
        return DriverManager.getConnection(config.jdbcUrl(), propriedades);
    }

    /**
     * Executa uma consulta trivial para validar a conexão e a configuração.
     */
    public void checkConnection() throws SQLException {
        try (Connection conexao = getConnection();
             PreparedStatement stmt = conexao.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("O banco de dados não respondeu à consulta de verificação.");
            }
        }
    }
}
