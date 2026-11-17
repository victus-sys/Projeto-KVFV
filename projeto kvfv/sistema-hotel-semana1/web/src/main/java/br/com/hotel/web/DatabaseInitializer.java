package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class DatabaseInitializer {
    @Value("${hotel.database.url}") private String databaseUrl;
    @Value("${hotel.admin.username}") private String adminUsername;
    @Value("${hotel.admin.password}") private String adminPassword;

    @Bean
    ApplicationRunner initializeDatabase(PasswordEncoder encoder) {
        return args -> {
            String normalizedAdminUsername = adminUsername.trim().toLowerCase(java.util.Locale.ROOT);
            if (databaseUrl.startsWith("jdbc:sqlite:")) {
                String file = databaseUrl.substring("jdbc:sqlite:".length());
                Path parent = Path.of(file).toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
            }
            try (Connection connection = DriverManager.getConnection(databaseUrl);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, login TEXT NOT NULL UNIQUE, senha TEXT NOT NULL, perfil TEXT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS hospede (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, documento TEXT, telefone TEXT, email TEXT, ativo INTEGER NOT NULL DEFAULT 1)");
                boolean hasActive = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(hospede)")) {
                    while (columns.next()) if ("ativo".equals(columns.getString("name"))) hasActive = true;
                }
                if (!hasActive) statement.executeUpdate("ALTER TABLE hospede ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS quarto (id INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE, tipo TEXT NOT NULL, status TEXT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS reserva (id INTEGER PRIMARY KEY AUTOINCREMENT, hospede_id INTEGER NOT NULL, quarto_id INTEGER NOT NULL, data_entrada TEXT NOT NULL, data_saida TEXT NOT NULL, status TEXT NOT NULL, FOREIGN KEY(hospede_id) REFERENCES hospede(id), FOREIGN KEY(quarto_id) REFERENCES quarto(id))");
                boolean hasRate=false, hasCapacity=false, hasDescription=false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(quarto)")) {
                    while (columns.next()) {
                        String name=columns.getString("name");
                        if("valor_diaria".equals(name)) hasRate=true;
                        if("capacidade".equals(name)) hasCapacity=true;
                        if("descricao".equals(name)) hasDescription=true;
                    }
                }
                if(!hasRate) statement.executeUpdate("ALTER TABLE quarto ADD COLUMN valor_diaria REAL NOT NULL DEFAULT 0");
                if(!hasCapacity) statement.executeUpdate("ALTER TABLE quarto ADD COLUMN capacidade INTEGER NOT NULL DEFAULT 1");
                if(!hasDescription) statement.executeUpdate("ALTER TABLE quarto ADD COLUMN descricao TEXT");
                statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reserva_quarto_periodo ON reserva(quarto_id, data_entrada, data_saida)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS hospedagem (id INTEGER PRIMARY KEY AUTOINCREMENT, reserva_id INTEGER NOT NULL UNIQUE, checkin_em TEXT NOT NULL, checkout_em TEXT, FOREIGN KEY(reserva_id) REFERENCES reserva(id))");
                boolean hasTotal=false;
                try(ResultSet columns=statement.executeQuery("PRAGMA table_info(hospedagem)")){while(columns.next())if("valor_total".equals(columns.getString("name")))hasTotal=true;}
                if(!hasTotal)statement.executeUpdate("ALTER TABLE hospedagem ADD COLUMN valor_total REAL NOT NULL DEFAULT 0");
                boolean hasAccountId = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(hospede)")) {
                    while (columns.next()) if ("usuario_id".equals(columns.getString("name"))) hasAccountId = true;
                }
                if (!hasAccountId) statement.executeUpdate("ALTER TABLE hospede ADD COLUMN usuario_id INTEGER REFERENCES usuario(id)");
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_hospede_usuario ON hospede(usuario_id) WHERE usuario_id IS NOT NULL");
                try (var query = connection.prepareStatement("SELECT id FROM usuario WHERE login = ?")) {
                    query.setString(1, normalizedAdminUsername);
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) {
                            try (var insert = connection.prepareStatement("INSERT INTO usuario (nome, login, senha, perfil) VALUES (?, ?, ?, ?)")) {
                                insert.setString(1, "Administrador");
                                insert.setString(2, normalizedAdminUsername);
                                insert.setString(3, encoder.encode(adminPassword));
                                insert.setString(4, "ADMIN");
                                insert.executeUpdate();
                            }
                        }
                    }
                }
            }
        };
    }
}
