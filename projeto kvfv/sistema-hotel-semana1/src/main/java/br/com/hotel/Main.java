
package br.com.hotel;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main extends Application {

    private final ObservableList<Hospede> hospedes =
            FXCollections.observableArrayList();

    private final TableView<Hospede> tabela = new TableView<>();

    @Override
    public void start(Stage stage) {
        Label titulo = new Label("Sistema de Gerenciamento de Hotel");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label subtitulo = new Label("Cadastrar hóspede");

        TextField nome = new TextField();
        nome.setPromptText("Nome completo");

        TextField documento = new TextField();
        documento.setPromptText("CPF ou documento");

        TextField telefone = new TextField();
        telefone.setPromptText("Telefone");

        TextField email = new TextField();
        email.setPromptText("E-mail");

        Button salvar = new Button("Cadastrar hóspede");
        Label mensagem = new Label();

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);

        formulario.add(new Label("Nome:"), 0, 0);
        formulario.add(nome, 1, 0);
        formulario.add(new Label("Documento:"), 0, 1);
        formulario.add(documento, 1, 1);
        formulario.add(new Label("Telefone:"), 0, 2);
        formulario.add(telefone, 1, 2);
        formulario.add(new Label("E-mail:"), 0, 3);
        formulario.add(email, 1, 3);

        salvar.setOnAction(event -> {
            if (nome.getText().isBlank()) {
                mensagem.setText("Informe o nome do hóspede.");
                return;
            }

            String sql = """
                INSERT INTO hospede (nome, documento, telefone, email)
                VALUES (?, ?, ?, ?)
                """;

            try (Connection conexao = Database.getConnection();
                 PreparedStatement comando = conexao.prepareStatement(sql)) {

                comando.setString(1, nome.getText().trim());
                comando.setString(2, documento.getText().trim());
                comando.setString(3, telefone.getText().trim());
                comando.setString(4, email.getText().trim());
                comando.executeUpdate();

                mensagem.setText("Hóspede cadastrado com sucesso!");
                nome.clear();
                documento.clear();
                telefone.clear();
                email.clear();

                carregarHospedes();

            } catch (SQLException erro) {
                mensagem.setText("Erro ao cadastrar: " + erro.getMessage());
                erro.printStackTrace();
            }
        });

        configurarTabela();

        Button atualizar = new Button("Atualizar lista");
        atualizar.setOnAction(event -> carregarHospedes());

        VBox layout = new VBox(
                14,
                titulo,
                subtitulo,
                formulario,
                salvar,
                mensagem,
                new Separator(),
                new Label("Hóspedes cadastrados"),
                tabela,
                atualizar
        );

        layout.setPadding(new Insets(22));

        Scene cena = new Scene(layout, 800, 650);
        stage.setTitle("Sistema de Hotel");
        stage.setScene(cena);
        stage.show();

        carregarHospedes();
    }

    private void configurarTabela() {
        TableColumn<Hospede, Number> colunaId = new TableColumn<>("ID");
        colunaId.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleIntegerProperty(
                        dado.getValue().getId()));

        TableColumn<Hospede, String> colunaNome = new TableColumn<>("Nome");
        colunaNome.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getNome()));

        TableColumn<Hospede, String> colunaDocumento =
                new TableColumn<>("Documento");
        colunaDocumento.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getDocumento()));

        TableColumn<Hospede, String> colunaTelefone =
                new TableColumn<>("Telefone");
        colunaTelefone.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getTelefone()));

        TableColumn<Hospede, String> colunaEmail =
                new TableColumn<>("E-mail");
        colunaEmail.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getEmail()));

        tabela.getColumns().setAll(
                colunaId, colunaNome, colunaDocumento,
                colunaTelefone, colunaEmail
        );

        tabela.setItems(hospedes);
        tabela.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        tabela.setPrefHeight(220);
    }

    private void carregarHospedes() {
        hospedes.clear();

        String sql = """
                SELECT id, nome, documento, telefone, email
                FROM hospede
                ORDER BY id DESC
                """;

        try (Connection conexao = Database.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {

            while (resultado.next()) {
                hospedes.add(new Hospede(
                        resultado.getInt("id"),
                        resultado.getString("nome"),
                        resultado.getString("documento"),
                        resultado.getString("telefone"),
                        resultado.getString("email")
                ));
            }

        } catch (SQLException erro) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Não foi possível carregar os hóspedes: "
                            + erro.getMessage()
            ).showAndWait();
            erro.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Database.initialize();
        launch(args);
    }

    public static class Hospede {
        private final int id;
        private final String nome;
        private final String documento;
        private final String telefone;
        private final String email;

        public Hospede(
                int id, String nome, String documento,
                String telefone, String email) {
            this.id = id;
            this.nome = nome;
            this.documento = documento;
            this.telefone = telefone;
            this.email = email;
        }

        public int getId() { return id; }
        public String getNome() { return nome; }
        public String getDocumento() { return documento; }
        public String getTelefone() { return telefone; }
        public String getEmail() { return email; }
    }
}
