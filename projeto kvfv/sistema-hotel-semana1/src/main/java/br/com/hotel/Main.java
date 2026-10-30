
package br.com.hotel;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main extends Application {

    private final ObservableList<Hospede> hospedes =
            FXCollections.observableArrayList();

    private final FilteredList<Hospede> hospedesFiltrados =
            new FilteredList<>(hospedes, hospede -> true);

    private final TableView<Hospede> tabela = new TableView<>();
    private Hospede hospedeEditando;

    @Override
    public void start(Stage stage) {

        Image logoImagem = new Image(
                getClass().getResourceAsStream("/images/logo-hotel-alba.jpeg")
        );
        ImageView logo = new ImageView(logoImagem);
        logo.setFitWidth(110);
        logo.setFitHeight(110);
        logo.setPreserveRatio(true);

        Label titulo = new Label("Hotel Alba");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        HBox cabecalho = new HBox(16, logo, titulo);
        cabecalho.setAlignment(Pos.CENTER_LEFT);

        Label subtitulo = new Label("Cadastrar hóspede");

        TextField nome = new TextField();
        nome.setPromptText("Nome completo");

        TextField documento = new TextField();
        documento.setPromptText("CPF ou documento");

        TextField telefone = new TextField();
        telefone.setPromptText("Telefone");

        TextField email = new TextField();
        email.setPromptText("E-mail");

        TextField busca = new TextField();
        busca.setPromptText("Buscar por nome ou documento");

        Button salvar = new Button("Cadastrar hóspede");
        Button cancelarEdicao = new Button("Cancelar edição");
        Button atualizar = new Button("Atualizar lista");
        Button editar = new Button("Editar hóspede selecionado");
        Button excluir = new Button("Excluir hóspede");

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

        cancelarEdicao.setVisible(false);
        cancelarEdicao.setManaged(false);

        busca.textProperty().addListener((observable, valorAntigo, valorNovo) -> {
            String termo = valorNovo == null
                    ? ""
                    : valorNovo.trim().toLowerCase();

            hospedesFiltrados.setPredicate(hospede ->
                    termo.isEmpty()
                            || hospede.getNome().toLowerCase().contains(termo)
                            || (hospede.getDocumento() != null
                            && hospede.getDocumento()
                            .toLowerCase().contains(termo))
            );
        });

        // CADASTRAR HÓSPEDE
        salvar.setOnAction(event -> {

            if (nome.getText().isBlank()) {
                mensagem.setText("Informe o nome do hóspede.");
                return;
            }

            String emailInformado = email.getText().trim();
            if (!emailInformado.isEmpty()
                    && !emailInformado.matches(
                    "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                mensagem.setText("Confira o formato do e-mail.");
                return;
            }

            String sql = hospedeEditando == null
                    ? "INSERT INTO hospede (nome, documento, telefone, email) "
                    + "VALUES (?, ?, ?, ?)"
                    : "UPDATE hospede SET nome = ?, documento = ?, "
                    + "telefone = ?, email = ? WHERE id = ?";

            try (Connection conexao = Database.getConnection();
                 PreparedStatement comando =
                         conexao.prepareStatement(sql)) {

                comando.setString(1, nome.getText().trim());
                comando.setString(2, documento.getText().trim());
                comando.setString(3, telefone.getText().trim());
                comando.setString(4, email.getText().trim());

                if (hospedeEditando != null) {
                    comando.setInt(5, hospedeEditando.getId());
                }

                int linhasAfetadas = comando.executeUpdate();

                if (linhasAfetadas == 0) {
                    mensagem.setText("O hóspede não foi encontrado.");
                    return;
                }

                mensagem.setText(hospedeEditando == null
                        ? "Hóspede cadastrado com sucesso!"
                        : "Dados do hóspede atualizados com sucesso!");

                nome.clear();
                documento.clear();
                telefone.clear();
                email.clear();
                hospedeEditando = null;
                salvar.setText("Cadastrar hóspede");
                cancelarEdicao.setVisible(false);
                cancelarEdicao.setManaged(false);

                carregarHospedes();

            } catch (SQLException erro) {
                mensagem.setText(
                        "Não foi possível salvar os dados: "
                                + erro.getMessage()
                );
                erro.printStackTrace();
            }
        });

        // CONFIGURAR TABELA
        configurarTabela();

        // ATUALIZAR LISTA
        atualizar.setOnAction(event -> carregarHospedes());

        editar.setOnAction(event -> {
            Hospede selecionado = tabela.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                mensagem.setText("Selecione um hóspede para editar.");
                return;
            }

            hospedeEditando = selecionado;
            nome.setText(selecionado.getNome());
            documento.setText(selecionado.getDocumento());
            telefone.setText(selecionado.getTelefone());
            email.setText(selecionado.getEmail());
            salvar.setText("Salvar alterações");
            cancelarEdicao.setVisible(true);
            cancelarEdicao.setManaged(true);
            mensagem.setText("Edite os dados e salve as alterações.");
        });

        cancelarEdicao.setOnAction(event -> {
            nome.clear();
            documento.clear();
            telefone.clear();
            email.clear();
            hospedeEditando = null;
            salvar.setText("Cadastrar hóspede");
            cancelarEdicao.setVisible(false);
            cancelarEdicao.setManaged(false);
            mensagem.setText("Edição cancelada.");
        });

        // EXCLUIR HÓSPEDE SELECIONADO
        excluir.setOnAction(event -> {

            Hospede selecionado =
                    tabela.getSelectionModel().getSelectedItem();

            if (selecionado == null) {
                mensagem.setText(
                        "Selecione um hóspede na tabela."
                );
                return;
            }

            Alert confirmacao =
                    new Alert(Alert.AlertType.CONFIRMATION);

            confirmacao.setTitle("Confirmar exclusão");
            confirmacao.setHeaderText("Excluir hóspede");
            confirmacao.setContentText(
                    "Deseja excluir o hóspede "
                            + selecionado.getNome() + "?"
            );

            if (confirmacao.showAndWait()
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {

                String sql =
                        "DELETE FROM hospede WHERE id = ?";

                try (Connection conexao = Database.getConnection();
                     PreparedStatement comando =
                             conexao.prepareStatement(sql)) {

                    comando.setInt(1, selecionado.getId());

                    int linhasAfetadas = comando.executeUpdate();

                    if (linhasAfetadas > 0) {
                        mensagem.setText(
                                "Hóspede excluído com sucesso!"
                        );
                    } else {
                        mensagem.setText(
                                "O hóspede não foi encontrado."
                        );
                    }

                    carregarHospedes();

                } catch (SQLException erro) {
                    mensagem.setText(
                            "Erro ao excluir: " + erro.getMessage()
                    );
                    erro.printStackTrace();
                }
            }
        });

        // MONTAR INTERFACE
        VBox layout = new VBox(
                14,
                cabecalho,
                subtitulo,
                formulario,
                salvar,
                cancelarEdicao,
                mensagem,
                new Separator(),
                new Label("Hóspedes cadastrados"),
                busca,
                tabela,
                atualizar,
                editar,
                excluir
        );

        layout.setPadding(new Insets(22));

        Scene cena = new Scene(layout, 800, 740);

        stage.setTitle("Hotel Alba - Sistema de Gerenciamento");
        stage.getIcons().add(logoImagem);
        stage.setScene(cena);
        stage.show();

        carregarHospedes();
    }

    // CONFIGURAR COLUNAS DA TABELA
    @SuppressWarnings("unchecked")
    private void configurarTabela() {

        TableColumn<Hospede, Number> colunaId =
                new TableColumn<>("ID");

        colunaId.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleIntegerProperty(
                        dado.getValue().getId()
                )
        );

        TableColumn<Hospede, String> colunaNome =
                new TableColumn<>("Nome");

        colunaNome.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getNome()
                )
        );

        TableColumn<Hospede, String> colunaDocumento =
                new TableColumn<>("Documento");

        colunaDocumento.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getDocumento()
                )
        );

        TableColumn<Hospede, String> colunaTelefone =
                new TableColumn<>("Telefone");

        colunaTelefone.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getTelefone()
                )
        );

        TableColumn<Hospede, String> colunaEmail =
                new TableColumn<>("E-mail");

        colunaEmail.setCellValueFactory(dado ->
                new javafx.beans.property.SimpleStringProperty(
                        dado.getValue().getEmail()
                )
        );

        tabela.getColumns().setAll(
                colunaId,
                colunaNome,
                colunaDocumento,
                colunaTelefone,
                colunaEmail
        );

        tabela.setItems(hospedesFiltrados);

        tabela.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        tabela.setPrefHeight(220);
    }

    // CARREGAR HÓSPEDES DO BANCO DE DADOS
    private void carregarHospedes() {

        hospedes.clear();

        String sql = """
                SELECT id, nome, documento, telefone, email
                FROM hospede
                ORDER BY id DESC
                """;

        try (Connection conexao = Database.getConnection();
             PreparedStatement comando =
                     conexao.prepareStatement(sql);
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

    // CLASSE QUE REPRESENTA UM HÓSPEDE
    public static class Hospede {

        private final int id;
        private final String nome;
        private final String documento;
        private final String telefone;
        private final String email;

        public Hospede(
                int id,
                String nome,
                String documento,
                String telefone,
                String email) {

            this.id = id;
            this.nome = nome;
            this.documento = documento;
            this.telefone = telefone;
            this.email = email;
        }

        public int getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }

        public String getDocumento() {
            return documento;
        }

        public String getTelefone() {
            return telefone;
        }

        public String getEmail() {
            return email;
        }
    }
}
