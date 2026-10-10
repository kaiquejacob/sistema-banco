package br.com.sistemabanco.infrastructure.config;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class ConexaoBanco {
    private static final String URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3307/banco_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");

    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "root");

    private static final HikariDataSource DATA_SOURCE = criarDataSource();

    private ConexaoBanco() {
    }

    private static HikariDataSource criarDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void fechar() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }

}
