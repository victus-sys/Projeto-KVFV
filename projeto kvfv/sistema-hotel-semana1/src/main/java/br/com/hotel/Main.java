
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
import java.sql.Statement;
import br.com.hotel.domain.Hospede;
import br.com.hotel.domain.PerfilAcesso;
import br.com.hotel.domain.Usuario;

public class Main extends Application {

    private final ObservableList<Hospede> hospedes =
            FXCollections.observableArrayList();

    private final FilteredList<Hospede> hospedesFiltrados =
            new FilteredList<>(hospedes, hospede -> true);

    private final TableView<Hospede> tabela = new TableView<>();
    private final ObservableList<Hospede> hospedesInativos=FXCollections.observableArrayList();
    private final TableView<Hospede> tabelaInativos=new TableView<>();
    private Hospede hospedeEditando;

    @Override
    public void start(Stage stage) {
        mostrarLogin(stage);
    }

    private void mostrarLogin(Stage stage) {
        Label titulo = new Label("Hotel Alba");
        titulo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        Label subtitulo = new Label("Acesso ao sistema");
        ComboBox<String> perfil = new ComboBox<>();
        perfil.getItems().setAll("Administrador", "Funcionário", "Hóspede");
        perfil.setValue("Administrador");
        TextField login = new TextField();
        login.setPromptText("E-mail ou usuário");
        PasswordField senha = new PasswordField();
        senha.setPromptText("Senha");
        Label mensagem = new Label();
        Button entrar = new Button("Entrar");
        entrar.setDefaultButton(true);
        Button cadastrar = new Button("Criar conta de hóspede");
        cadastrar.setOnAction(event -> mostrarCadastroHospede(stage));
        entrar.setOnAction(event -> {
            if (login.getText().isBlank() || senha.getText().isBlank()) {
                mensagem.setText("Informe o usuário e a senha.");
                return;
            }
            try {
                Usuario usuario = Database.autenticar(login.getText(), senha.getText());
                if (usuario == null) {
                    mensagem.setText("Usuário ou senha inválidos.");
                    return;
                }
                PerfilAcesso perfilEscolhido = switch (perfil.getValue()) {
                    case "Administrador" -> PerfilAcesso.ADMIN;
                    case "Funcionário" -> PerfilAcesso.RECEPCIONISTA;
                    default -> PerfilAcesso.HOSPEDE;
                };
                if (usuario.perfil() != perfilEscolhido) {
                    mensagem.setText("Esse acesso não corresponde ao perfil escolhido.");
                    return;
                }
                if (usuario.perfil() == PerfilAcesso.HOSPEDE) {
                    mostrarAreaHospede(stage, usuario);
                } else {
                    mostrarGestaoHospedes(stage, usuario);
                }
            } catch (SQLException erro) {
                mensagem.setText("Não foi possível acessar o sistema: " + erro.getMessage());
            }
        });
        VBox layout = new VBox(14, titulo, subtitulo, new Label("Entrar como"), perfil, new Label("Usuário"), login,
                new Label("Senha"), senha, entrar, cadastrar, mensagem);
        layout.setPadding(new Insets(32));
        layout.setMaxWidth(420);
        VBox centralizado = new VBox(layout);
        centralizado.setAlignment(Pos.CENTER);
        stage.setTitle("Hotel Alba - Entrar");
        stage.setScene(new Scene(centralizado, 560, 500));
        stage.show();
    }

    private void mostrarCadastroHospede(Stage stage) {
        TextField nome=new TextField();nome.setPromptText("Nome completo");TextField email=new TextField();email.setPromptText("E-mail de acesso");
        PasswordField senha=new PasswordField();senha.setPromptText("Senha (mínimo 6 caracteres)");PasswordField confirmar=new PasswordField();confirmar.setPromptText("Confirme a senha");Label mensagem=new Label();
        Button salvar=new Button("Criar conta");salvar.setDefaultButton(true);salvar.setOnAction(event->{
            if(nome.getText().isBlank()||!email.getText().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||senha.getText().length()<6||!senha.getText().equals(confirmar.getText())){mensagem.setText("Confira o nome, e-mail e senha (mínimo 6 caracteres).");return;}
            try(Connection c=Database.getConnection()){
                c.setAutoCommit(false);
                try{
                    int userId;try(PreparedStatement p=c.prepareStatement("INSERT INTO usuario(nome,login,senha,perfil) VALUES(?,?,?,'HOSPEDE')")){p.setString(1,nome.getText().trim());p.setString(2,email.getText().trim().toLowerCase(java.util.Locale.ROOT));p.setString(3,br.com.hotel.security.PasswordHasher.hash(senha.getText()));p.executeUpdate();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT last_insert_rowid()")){r.next();userId=r.getInt(1);}}
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO hospede(nome,email,usuario_id) VALUES(?,?,?)")){p.setString(1,nome.getText().trim());p.setString(2,email.getText().trim().toLowerCase(java.util.Locale.ROOT));p.setInt(3,userId);p.executeUpdate();}
                    c.commit();new Alert(Alert.AlertType.INFORMATION,"Conta criada. Entre com seu e-mail e senha.").showAndWait();mostrarLogin(stage);
                }catch(Exception e){c.rollback();throw new RuntimeException(e);}
            }catch(Exception e){mensagem.setText("Não foi possível criar a conta. Confira se o e-mail já está cadastrado.");}
        });
        Button voltar=new Button("Voltar ao login");voltar.setOnAction(event->mostrarLogin(stage));VBox layout=new VBox(12,new Label("Cadastro de hóspede"),nome,email,senha,confirmar,salvar,mensagem,voltar);layout.setPadding(new Insets(36));layout.setMaxWidth(440);VBox center=new VBox(layout);center.setAlignment(Pos.CENTER);stage.setTitle("Hotel Alba - Cadastro");stage.setScene(new Scene(center,560,560));stage.show();
    }

    private void mostrarAreaHospede(Stage stage, Usuario usuario) {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(28));
        Label titulo = new Label("Hotel Alba - Área do hóspede");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        layout.getChildren().add(titulo);
        try {
            Hospede hospede = Database.buscarHospedeDoUsuario(usuario.id());
            if (hospede == null) {
                layout.getChildren().add(new Label("Não encontramos um cadastro de hóspede vinculado a esta conta."));
            } else {
                layout.getChildren().addAll(new Label("Nome: " + hospede.getNome()),
                        new Label("Documento: " + valorOuTraco(hospede.getDocumento())),
                        new Label("Telefone: " + valorOuTraco(hospede.getTelefone())),
                        new Label("E-mail: " + valorOuTraco(hospede.getEmail())));
            }
        } catch (SQLException erro) {
            layout.getChildren().add(new Label("Não foi possível carregar seus dados: " + erro.getMessage()));
        }
        Button minhasReservas = new Button("Ver quartos e minhas reservas");
        minhasReservas.setOnAction(event -> TelaReservas.mostrar(stage, usuario, () -> mostrarAreaHospede(stage, usuario)));
        layout.getChildren().add(minhasReservas);
        Button minhaConta = new Button("Editar meus dados e senha");
        minhaConta.setOnAction(event -> TelaContaHospede.mostrar(stage, usuario, () -> mostrarAreaHospede(stage, usuario)));
        layout.getChildren().add(minhaConta);
        Button sair = new Button("Sair");
        sair.setOnAction(event -> mostrarLogin(stage));
        layout.getChildren().add(sair);
        stage.setTitle("Hotel Alba - Área do hóspede");
        stage.setScene(new Scene(layout, 650, 420));
        stage.show();
    }

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor;
    }

    private void mostrarGestaoHospedes(Stage stage, Usuario usuario) {

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

        Label sessao = new Label(usuario.nome() + " · " + usuario.perfil());
        Button sair = new Button("Sair");
        sair.setOnAction(event -> mostrarLogin(stage));
        HBox sessaoBarra = new HBox(12, sessao, sair);
        sessaoBarra.setAlignment(Pos.CENTER_RIGHT);

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
        boolean equipe = usuario.perfil() == PerfilAcesso.ADMIN || usuario.perfil() == PerfilAcesso.RECEPCIONISTA;
        excluir.setVisible(equipe);
        excluir.setManaged(equipe);
        Button reservasButton = new Button("Quartos e reservas");
        reservasButton.setOnAction(event -> TelaReservas.mostrar(stage, usuario, () -> mostrarGestaoHospedes(stage, usuario)));
        Button adminButton = new Button("Administração");
        boolean administrador = usuario.perfil() == PerfilAcesso.ADMIN;
        adminButton.setVisible(administrador);
        adminButton.setManaged(administrador);
        adminButton.setOnAction(event -> TelaAdmin.mostrar(stage, usuario, () -> mostrarGestaoHospedes(stage, usuario)));

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

            try {
                Database.salvarHospede(hospedeEditando == null ? null : hospedeEditando.getId(),
                        nome.getText(), documento.getText(), telefone.getText(), email.getText());
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

            } catch (SQLException | IllegalArgumentException erro) {
                mensagem.setText(
                        "Não foi possível salvar os dados: "
                                + erro.getMessage()
                );
                erro.printStackTrace();
            }
        });

        // CONFIGURAR TABELA
        configurarTabela(tabela,hospedesFiltrados,220);
        configurarTabela(tabelaInativos,hospedesInativos,120);
        Button apagarInativo=new Button("Apagar definitivamente hóspede selecionado");
        apagarInativo.setOnAction(event->{
            Hospede selecionado=tabelaInativos.getSelectionModel().getSelectedItem();
            if(selecionado==null){mensagem.setText("Selecione um hóspede removido.");return;}
            Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Apagar este hóspede e todas as reservas ligadas a ele definitivamente?",ButtonType.CANCEL,ButtonType.OK);
            confirm.setHeaderText("Exclusão definitiva do banco de dados");
            if(confirm.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK){
                try{int count=Database.excluirHospede(selecionado.getId());mensagem.setText("Registro apagado definitivamente ("+count+" reserva(s)).");carregarHospedes();}
                catch(SQLException|IllegalStateException|IllegalArgumentException ex){mensagem.setText("Não foi possível apagar: "+ex.getMessage());}
            }
        });
        TitledPane removidos=new TitledPane("Registros removidos anteriormente ainda no banco",new VBox(8,tabelaInativos,apagarInativo));
        removidos.setExpanded(false);

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
                            + selecionado.getNome() + " e todas as reservas ligadas a ele? Esta ação não pode ser desfeita."
            );

            if (confirmacao.showAndWait()
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {

                try {
                    if (usuario.perfil() != PerfilAcesso.ADMIN && usuario.perfil() != PerfilAcesso.RECEPCIONISTA) throw new IllegalStateException("Seu perfil não pode excluir hóspedes.");
                    int reservasExcluidas=Database.excluirHospede(selecionado.getId());
                    mensagem.setText("Hóspede excluído definitivamente junto com " + reservasExcluidas + " reserva(s).");
                    carregarHospedes();
                } catch (SQLException | IllegalStateException | IllegalArgumentException erro) {
                    mensagem.setText("Não foi possível excluir: " + erro.getMessage());
                }
            }
        });

        // MONTAR INTERFACE
        VBox layout = new VBox(
                14,
                cabecalho,
                sessaoBarra,
                subtitulo,
                formulario,
                salvar,
                cancelarEdicao,
                mensagem,
                new Separator(),
                new Label("Hóspedes cadastrados"),
                busca,
                tabela,
                removidos,
                atualizar,
                editar,
                excluir,
                reservasButton,
                adminButton
        );

        layout.setPadding(new Insets(22));

        Scene cena = new Scene(layout, 800, 790);

        stage.setTitle("Hotel Alba - Sistema de Gerenciamento");
        stage.getIcons().add(logoImagem);
        stage.setScene(cena);
        stage.show();

        carregarHospedes();
    }

    // CONFIGURAR COLUNAS DA TABELA
    @SuppressWarnings("unchecked")
    private void configurarTabela(TableView<Hospede> alvo,ObservableList<Hospede> itens,double altura) {

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

        alvo.getColumns().setAll(
                colunaId,
                colunaNome,
                colunaDocumento,
                colunaTelefone,
                colunaEmail
        );

        alvo.setItems(itens);

        alvo.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        alvo.setPrefHeight(altura);
    }

    // CARREGAR HÓSPEDES DO BANCO DE DADOS
    private void carregarHospedes() {

        hospedes.clear();

        String sql = """
                SELECT id, nome, documento, telefone, email
                FROM hospede
                WHERE ativo=1
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
        try{hospedesInativos.setAll(Database.buscarHospedesInativos());}
        catch(SQLException erro){new Alert(Alert.AlertType.ERROR,"Não foi possível carregar os hóspedes removidos: "+erro.getMessage()).showAndWait();}
    }

    public static void main(String[] args) {
        Database.initialize();
        launch(args);
    }

}
