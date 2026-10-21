package br.com.hotel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String URL = "jdbc:sqlite:data/hotel.db";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try {
            java.nio.file.Files.createDirectories(
                    java.nio.file.Paths.get("data")
            );

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS usuario (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL," +
                    "login TEXT NOT NULL UNIQUE," +
                    "senha TEXT NOT NULL," +
                    "perfil TEXT NOT NULL" +
                    ")"
                );

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS hospede (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT NOT NULL," +
                    "documento TEXT," +
                    "telefone TEXT," +
                    "email TEXT" +
                    ")"
                );

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS quarto (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "numero TEXT NOT NULL UNIQUE," +
                    "tipo TEXT NOT NULL," +
                    "status TEXT NOT NULL" +
                    ")"
                );

                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS reserva (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "hospede_id INTEGER NOT NULL," +
                    "quarto_id INTEGER NOT NULL," +
                    "data_entrada TEXT NOT NULL," +
                    "data_saida TEXT NOT NULL," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(hospede_id) REFERENCES hospede(id)," +
                    "FOREIGN KEY(quarto_id) REFERENCES quarto(id)" +
                    ")"
                );
            }

            System.out.println("Banco SQLite inicializado com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro ao inicializar o banco: " + e.getMessage());
        }
    }
}
