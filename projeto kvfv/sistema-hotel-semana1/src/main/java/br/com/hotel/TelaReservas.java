package br.com.hotel;

import br.com.hotel.domain.PerfilAcesso;
import br.com.hotel.domain.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Telas simples de consulta e operação das reservas, compartilhando o SQLite local. */
public final class TelaReservas {
    private final Usuario usuario;
    private final boolean hospede;
    private final Integer hospedeLogadoId;
    private final TableView<LinhaReserva> tabela = new TableView<>();
    private final ComboBox<OpcaoHospede> seletorHospede = new ComboBox<>();
    private final DatePicker entrada = new DatePicker(LocalDate.now().plusDays(1));
    private final DatePicker saida = new DatePicker(LocalDate.now().plusDays(2));
    private final Label mensagem = new Label();
    private final GridPane mapa = new GridPane();
    private Integer quartoSelecionado;

    private TelaReservas(Usuario usuario) throws SQLException {
        this.usuario = usuario;
        this.hospede = usuario.perfil() == PerfilAcesso.HOSPEDE;
        var cadastro = hospede ? Database.buscarHospedeDoUsuario(usuario.id()) : null;
        this.hospedeLogadoId = cadastro == null ? null : cadastro.getId();
    }

    public static void mostrar(Stage stage, Usuario usuario, Runnable voltar) {
        try { new TelaReservas(usuario).montar(stage, voltar); }
        catch (SQLException e) { new Alert(Alert.AlertType.ERROR, "Não foi possível abrir as reservas: " + e.getMessage()).showAndWait(); }
    }

    private void montar(Stage stage, Runnable voltar) throws SQLException {
        Label titulo = new Label(hospede ? "Quartos e minhas reservas" : "Reservas do hotel");
        titulo.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");
        Button voltarButton = new Button("Voltar"); voltarButton.setOnAction(e -> voltar.run());
        HBox topo = new HBox(12, voltarButton, titulo, new Region()); HBox.setHgrow(topo.getChildren().get(2), Priority.ALWAYS); topo.setAlignment(Pos.CENTER_LEFT);

        if (!hospede) { seletorHospede.setItems(FXCollections.observableArrayList(carregarHospedes())); seletorHospede.setPromptText("Escolha o hóspede"); }
        GridPane periodo = new GridPane(); periodo.setHgap(10); periodo.setVgap(8);
        periodo.add(new Label("Entrada"),0,0); periodo.add(entrada,0,1); periodo.add(new Label("Saída"),1,0); periodo.add(saida,1,1);
        Button atualizarMapa = new Button("Consultar quartos"); atualizarMapa.setOnAction(e -> carregarMapa());
        Button novaReserva = new Button("Nova reserva"); novaReserva.setOnAction(e -> { tabela.getSelectionModel().clearSelection(); quartoSelecionado=null; if(!hospede) seletorHospede.setValue(null); mensagem.setText("Preencha os dados para uma nova reserva."); });
        Button reservar = new Button("Confirmar reserva"); reservar.setDefaultButton(false); reservar.setOnAction(e -> salvarReserva());
        VBox formulario = new VBox(10, new Label(hospede ? "Selecione datas e um quarto livre" : "Nova reserva / alteração"));
        if (!hospede) formulario.getChildren().add(seletorHospede);
        formulario.getChildren().addAll(periodo, atualizarMapa, mapa, reservar, mensagem);
        formulario.setPadding(new Insets(16)); formulario.setStyle("-fx-background-color: white; -fx-border-color: #e7e0ed; -fx-border-radius: 10; -fx-background-radius: 10;");

        configurarTabela(); carregarReservas(); carregarMapa();
        Button alterar = new Button("Carregar para alteração"); alterar.setOnAction(e -> carregarParaAlterar());
        Button cancelar = new Button("Cancelar reserva selecionada"); cancelar.setOnAction(e -> cancelarReserva());
        Button confirmar = new Button("Confirmar reserva");confirmar.setOnAction(e->confirmarReserva());
        Button checkin = new Button("Check-in");checkin.setOnAction(e->checkin());
        Button checkout = new Button("Check-out");checkout.setOnAction(e->checkout());
        Button apagarCanceladas=new Button("Apagar reservas canceladas");apagarCanceladas.setOnAction(e->apagarReservasCanceladas());apagarCanceladas.setVisible(!hospede);apagarCanceladas.setManaged(!hospede);
        for(Button b:List.of(confirmar,checkin,checkout)){b.setVisible(!hospede);b.setManaged(!hospede);}
        HBox acoes = new HBox(10, novaReserva, alterar, cancelar, confirmar, checkin, checkout,apagarCanceladas);
        VBox lista = new VBox(10, new Label(hospede ? "Meu histórico de reservas" : "Reservas cadastradas"), tabela, acoes); VBox.setVgrow(tabela, Priority.ALWAYS);
        HBox corpo = new HBox(16, formulario, lista); HBox.setHgrow(lista, Priority.ALWAYS); formulario.setPrefWidth(390);
        VBox layout = new VBox(16, topo, corpo); layout.setPadding(new Insets(20));
        stage.setTitle("Hotel Alba - Reservas"); stage.setScene(new Scene(layout, 1120, 760)); stage.show();
    }

    private List<OpcaoHospede> carregarHospedes() throws SQLException {
        List<OpcaoHospede> result = new ArrayList<>();
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement("SELECT id,nome FROM hospede WHERE ativo=1 ORDER BY nome"); ResultSet r=p.executeQuery()) { while(r.next()) result.add(new OpcaoHospede(r.getInt("id"),r.getString("nome"))); }
        return result;
    }

    private void carregarMapa() {
        mapa.getChildren().clear(); mapa.setHgap(8); mapa.setVgap(8); mapa.setPadding(new Insets(8));
        if (entrada.getValue()==null || saida.getValue()==null || !saida.getValue().isAfter(entrada.getValue())) { mensagem.setText("A saída deve ocorrer depois da entrada."); return; }
        String sql="SELECT q.id,q.numero,q.tipo,q.status,q.valor_diaria,q.capacidade,q.descricao,NOT EXISTS (SELECT 1 FROM reserva r WHERE r.quarto_id=q.id AND r.status NOT IN ('CANCELADA','CONCLUIDA') AND r.data_entrada < ? AND r.data_saida > ?) AS livre FROM quarto q ORDER BY q.numero";
        int coluna=0, linha=0;
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,saida.getValue().toString()); p.setString(2,entrada.getValue().toString());
            try(ResultSet r=p.executeQuery()) { while(r.next()) {
                double diaria=r.getDouble("valor_diaria");boolean disponivel=!"MANUTENCAO".equals(r.getString("status")) && r.getInt("livre")==1 && diaria>0;
                Button quarto=new Button("Quarto " + r.getString("numero") + "\n" + (disponivel?"Disponível":"Indisponível") + "\n" + r.getString("tipo") + " · R$ " + String.format("%.2f",diaria) + "\n" + r.getInt("capacidade") + " pessoa(s)");
                quarto.setPrefSize(155,78); quarto.setWrapText(true); quarto.setStyle(disponivel?"-fx-background-color: #e8f6ef; -fx-border-color: #37a477; -fx-border-radius: 8;":"-fx-background-color: #f0eef2; -fx-text-fill: #777; -fx-border-color: #bbb; -fx-border-radius: 8;");
                int id=r.getInt("id"); quarto.setDisable(!disponivel); quarto.setOnAction(e->{quartoSelecionado=id; quarto.setStyle("-fx-background-color: #e8f6ef; -fx-border-color: #5d17eb; -fx-border-width: 2; -fx-border-radius: 8;"); mensagem.setText("Quarto " + rSafeRoomNumber(id) + " selecionado.");});
                mapa.add(quarto,coluna,linha); if(++coluna==2){coluna=0;linha++;}
            }}
            mensagem.setText("Verde: disponível para as datas. Cinza: indisponível.");
        } catch(SQLException e) { mensagem.setText("Erro ao consultar quartos: " + e.getMessage()); }
    }

    private String rSafeRoomNumber(int id) {
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT numero FROM quarto WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):String.valueOf(id);}}catch(SQLException e){return String.valueOf(id);}
    }

    private void salvarReserva() {
        try {
            if (quartoSelecionado==null) throw new IllegalArgumentException("Selecione um quarto disponível no mapa.");
            if (entrada.getValue()==null || saida.getValue()==null || entrada.getValue().isBefore(LocalDate.now()) || !saida.getValue().isAfter(entrada.getValue())) throw new IllegalArgumentException("Informe um período válido, a partir de hoje.");
            int dono;
            if(hospede){ if(hospedeLogadoId==null) throw new IllegalStateException("Sua conta ainda não está vinculada a um cadastro de hóspede."); dono=hospedeLogadoId; }
            else { if(seletorHospede.getValue()==null) throw new IllegalArgumentException("Escolha o hóspede da reserva."); dono=seletorHospede.getValue().id(); }
            LinhaReserva selecionada=tabela.getSelectionModel().getSelectedItem();
            try(Connection c=Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    String check="SELECT q.status,q.valor_diaria,NOT EXISTS(SELECT 1 FROM reserva r WHERE r.quarto_id=q.id AND r.status NOT IN ('CANCELADA','CONCLUIDA') AND r.data_entrada < ? AND r.data_saida > ?"+(selecionada==null?"":" AND r.id<>?")+") AS livre FROM quarto q WHERE q.id=?";
                    try(PreparedStatement p=c.prepareStatement(check)){p.setString(1,saida.getValue().toString());p.setString(2,entrada.getValue().toString());int i=3;if(selecionada!=null)p.setInt(i++,selecionada.id());p.setInt(i,quartoSelecionado);try(ResultSet r=p.executeQuery()){if(!r.next()||"MANUTENCAO".equals(r.getString("status"))||r.getDouble("valor_diaria")<=0||r.getInt("livre")!=1)throw new IllegalStateException("Esse quarto não está disponível ou ainda não tem diária cadastrada.");}}
                    try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM hospede WHERE id=? AND ativo=1")){p.setInt(1,dono);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Este hóspede não está ativo no cadastro.");}}
                    String ocupacao="SELECT 1 FROM reserva WHERE hospede_id=? AND status NOT IN ('CANCELADA','CONCLUIDA') AND data_entrada < ? AND data_saida > ?"+(selecionada==null?"":" AND id<>?")+" LIMIT 1";
                    try(PreparedStatement p=c.prepareStatement(ocupacao)){p.setInt(1,dono);p.setString(2,saida.getValue().toString());p.setString(3,entrada.getValue().toString());if(selecionada!=null)p.setInt(4,selecionada.id());try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalStateException("Este hóspede já tem outra reserva ativa nesse período. Um hóspede pode reservar apenas um quarto por vez.");}}
                    if(selecionada==null){try(PreparedStatement p=c.prepareStatement("INSERT INTO reserva(hospede_id,quarto_id,data_entrada,data_saida,status) VALUES(?,?,?,?, 'PENDENTE')")){p.setInt(1,dono);p.setInt(2,quartoSelecionado);p.setString(3,entrada.getValue().toString());p.setString(4,saida.getValue().toString());p.executeUpdate();}}
                    else {try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET hospede_id=?,quarto_id=?,data_entrada=?,data_saida=? WHERE id=? AND status IN ('PENDENTE','CONFIRMADA')")){p.setInt(1,dono);p.setInt(2,quartoSelecionado);p.setString(3,entrada.getValue().toString());p.setString(4,saida.getValue().toString());p.setInt(5,selecionada.id());if(p.executeUpdate()==0)throw new IllegalStateException("A reserva não pode mais ser alterada.");}}
                    c.commit();
                }catch(Exception e){c.rollback();throw new RuntimeException(e);}
            }
            mensagem.setText(selecionada==null?"Reserva confirmada.":"Reserva atualizada."); tabela.getSelectionModel().clearSelection(); carregarReservas(); carregarMapa(); quartoSelecionado=null;
        }catch(Exception e){mensagem.setText(e.getMessage());}
    }

    private void configurarTabela() {
        TableColumn<LinhaReserva,String> hospedeCol=new TableColumn<>("Hóspede");hospedeCol.setCellValueFactory(d->new SimpleStringProperty(d.getValue().hospede()));
        TableColumn<LinhaReserva,String> quartoCol=new TableColumn<>("Quarto");quartoCol.setCellValueFactory(d->new SimpleStringProperty(d.getValue().quarto()));
        TableColumn<LinhaReserva,String> entradaCol=new TableColumn<>("Entrada");entradaCol.setCellValueFactory(d->new SimpleStringProperty(d.getValue().entrada()));
        TableColumn<LinhaReserva,String> saidaCol=new TableColumn<>("Saída");saidaCol.setCellValueFactory(d->new SimpleStringProperty(d.getValue().saida()));
        TableColumn<LinhaReserva,String> statusCol=new TableColumn<>("Status");statusCol.setCellValueFactory(d->new SimpleStringProperty(d.getValue().status()));
        tabela.getColumns().setAll(hospedeCol,quartoCol,entradaCol,saidaCol,statusCol);tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);tabela.setPrefHeight(450);
        if(hospede){hospedeCol.setVisible(false);}
    }

    private void carregarReservas() {
        List<LinhaReserva> itens=new ArrayList<>();String sql="SELECT r.id,r.hospede_id,h.nome,q.numero,r.data_entrada,r.data_saida,r.status FROM reserva r JOIN hospede h ON h.id=r.hospede_id JOIN quarto q ON q.id=r.quarto_id "+(hospede?"WHERE r.hospede_id=? ":"")+"ORDER BY r.data_entrada DESC";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){if(hospede&&hospedeLogadoId!=null)p.setInt(1,hospedeLogadoId);try(ResultSet r=p.executeQuery()){while(r.next())itens.add(new LinhaReserva(r.getInt("id"),r.getInt("hospede_id"),r.getString("nome"),r.getString("numero"),r.getString("data_entrada"),r.getString("data_saida"),r.getString("status")));}tabela.setItems(FXCollections.observableArrayList(itens));}catch(SQLException e){mensagem.setText("Erro ao carregar reservas: "+e.getMessage());}
    }

    private void carregarParaAlterar() {
        LinhaReserva r=tabela.getSelectionModel().getSelectedItem();if(r==null){mensagem.setText("Selecione uma reserva.");return;}if(!List.of("PENDENTE","CONFIRMADA").contains(r.status())){mensagem.setText("Apenas reservas pendentes ou confirmadas podem ser alteradas.");return;}
        if(!hospede){for(OpcaoHospede h:seletorHospede.getItems())if(h.id()==r.hospedeId()){seletorHospede.setValue(h);break;}}
        entrada.setValue(LocalDate.parse(r.entrada()));saida.setValue(LocalDate.parse(r.saida()));carregarMapa();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT quarto_id FROM reserva WHERE id=?")){p.setInt(1,r.id());try(ResultSet rs=p.executeQuery()){if(rs.next())quartoSelecionado=rs.getInt(1);}}
        catch(SQLException e){mensagem.setText("Não foi possível carregar a reserva.");}
        mensagem.setText("Altere os dados e confirme para salvar.");
    }

    private void cancelarReserva() {
        LinhaReserva r=tabela.getSelectionModel().getSelectedItem();if(r==null){mensagem.setText("Selecione uma reserva.");return;}
        if(!List.of("PENDENTE","CONFIRMADA").contains(r.status())){mensagem.setText("A reserva não pode mais ser cancelada.");return;}
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                int room;
                String sql="SELECT quarto_id FROM reserva WHERE id=? AND status IN ('PENDENTE','CONFIRMADA')"+(hospede?" AND hospede_id=?":"");
                try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,r.id());if(hospede)p.setInt(2,hospedeLogadoId==null?-1:hospedeLogadoId);try(ResultSet rs=p.executeQuery()){if(!rs.next())throw new IllegalStateException("Não foi possível cancelar esta reserva.");room=rs.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='CANCELADA' WHERE id=?")){p.setInt(1,r.id());p.executeUpdate();}
                atualizarEstadoQuarto(c,room);c.commit();mensagem.setText("Reserva cancelada. O quarto está livre para outras datas.");carregarReservas();carregarMapa();
            }catch(Exception e){c.rollback();throw e;}
        }catch(Exception e){mensagem.setText("Erro ao cancelar: "+e.getMessage());}
    }

    private void apagarReservasCanceladas(){
        if(hospede){mensagem.setText("Apenas a equipe pode apagar reservas canceladas.");return;}
        Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Apagar definitivamente todas as reservas canceladas?",ButtonType.CANCEL,ButtonType.OK);
        confirm.setHeaderText("Limpar reservas canceladas");
        if(confirm.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try(Statement s=c.createStatement()){
                List<Integer> affectedRooms=new ArrayList<>();
                try(ResultSet rooms=s.executeQuery("SELECT DISTINCT quarto_id FROM reserva WHERE status='CANCELADA'")){while(rooms.next())affectedRooms.add(rooms.getInt(1));}
                for(int room:affectedRooms)atualizarEstadoQuarto(c,room);
                s.executeUpdate("DELETE FROM hospedagem WHERE reserva_id IN (SELECT id FROM reserva WHERE status='CANCELADA')");
                int total=s.executeUpdate("DELETE FROM reserva WHERE status='CANCELADA'");c.commit();
                mensagem.setText(total==0?"Não há reservas canceladas para apagar.":total+" reserva(s) cancelada(s) apagada(s).");carregarReservas();carregarMapa();
            }catch(Exception e){c.rollback();throw e;}
        }catch(Exception e){mensagem.setText("Não foi possível apagar reservas canceladas: "+e.getMessage());}
    }

    private void atualizarEstadoQuarto(Connection c,int quartoId)throws SQLException{
        boolean ocupado;
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,quartoId);try(ResultSet r=p.executeQuery()){ocupado=r.next();}}
        try(PreparedStatement p=c.prepareStatement("UPDATE quarto SET status=? WHERE id=? AND status<>'MANUTENCAO'")){p.setString(1,ocupado?"OCUPADO":"DISPONIVEL");p.setInt(2,quartoId);p.executeUpdate();}
    }
    private boolean existeHospedagemAtiva(Connection c,int quartoId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,quartoId);try(ResultSet r=p.executeQuery()){return r.next();}}
    }

    private void confirmarReserva(){
        if(hospede){mensagem.setText("Esta ação é exclusiva da equipe.");return;}LinhaReserva r=tabela.getSelectionModel().getSelectedItem();if(r==null){mensagem.setText("Selecione uma reserva.");return;}
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='CONFIRMADA' WHERE id=? AND status='PENDENTE'")){p.setInt(1,r.id());if(p.executeUpdate()==0)throw new IllegalStateException("A reserva não está pendente.");mensagem.setText("Reserva confirmada.");carregarReservas();carregarMapa();}catch(Exception e){mensagem.setText(e.getMessage());}
    }

    private void checkin(){
        if(hospede){mensagem.setText("Esta ação é exclusiva da equipe.");return;}LinhaReserva row=tabela.getSelectionModel().getSelectedItem();if(row==null){mensagem.setText("Selecione uma reserva.");return;}
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            int room;LocalDate start,end;String roomStatus;
            try(PreparedStatement p=c.prepareStatement("SELECT r.quarto_id,r.data_entrada,r.data_saida,r.status,q.status AS quarto_status,q.valor_diaria FROM reserva r JOIN quarto q ON q.id=r.quarto_id WHERE r.id=?")){p.setInt(1,row.id());try(ResultSet r=p.executeQuery()){if(!r.next()||!"CONFIRMADA".equals(r.getString("status")))throw new IllegalStateException("O check-in exige reserva confirmada.");room=r.getInt("quarto_id");start=LocalDate.parse(r.getString("data_entrada"));end=LocalDate.parse(r.getString("data_saida"));roomStatus=r.getString("quarto_status");if(r.getDouble("valor_diaria")<=0)throw new IllegalStateException("Cadastre o valor da diária antes do check-in.");}}
            LocalDate today=LocalDate.now();if(today.isBefore(start)||!today.isBefore(end))throw new IllegalStateException("O check-in só pode ocorrer dentro do período reservado.");if("MANUTENCAO".equals(roomStatus)||existeHospedagemAtiva(c,room))throw new IllegalStateException("O quarto não está disponível.");
            try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='EM_HOSPEDAGEM' WHERE id=? AND status='CONFIRMADA'")){p.setInt(1,row.id());if(p.executeUpdate()==0)throw new IllegalStateException("A reserva não pode receber check-in.");}
            try(PreparedStatement p=c.prepareStatement("UPDATE quarto SET status='OCUPADO' WHERE id=?")){p.setInt(1,room);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO hospedagem(reserva_id,checkin_em,valor_total) VALUES(?,?,0)")){p.setInt(1,row.id());p.setString(2,java.time.LocalDateTime.now().toString());p.executeUpdate();}
            c.commit();mensagem.setText("Check-in realizado.");carregarReservas();carregarMapa();
        }catch(Exception e){c.rollback();throw new RuntimeException(e);}}catch(Exception e){mensagem.setText(e.getMessage());}
    }

    private void checkout(){
        if(hospede){mensagem.setText("Esta ação é exclusiva da equipe.");return;}LinhaReserva row=tabela.getSelectionModel().getSelectedItem();if(row==null){mensagem.setText("Selecione uma hospedagem.");return;}
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            int room;String checkin;
            try(PreparedStatement p=c.prepareStatement("SELECT r.quarto_id,h.checkin_em FROM reserva r JOIN hospedagem h ON h.reserva_id=r.id WHERE r.id=? AND r.status='EM_HOSPEDAGEM' AND h.checkout_em IS NULL")){p.setInt(1,row.id());try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("A reserva não possui hospedagem ativa.");room=r.getInt(1);checkin=r.getString(2);}}
            double daily;try(PreparedStatement p=c.prepareStatement("SELECT valor_diaria FROM quarto WHERE id=?")){p.setInt(1,room);try(ResultSet r=p.executeQuery()){r.next();daily=r.getDouble(1);}}
            LocalDate checkinDate=LocalDate.parse(checkin.substring(0,10));long nights=Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(checkinDate,LocalDate.now()));
            try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='CONCLUIDA' WHERE id=? AND status='EM_HOSPEDAGEM'")){p.setInt(1,row.id());p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE hospedagem SET checkout_em=?,valor_total=? WHERE reserva_id=? AND checkout_em IS NULL")){p.setString(1,java.time.LocalDateTime.now().toString());p.setDouble(2,nights*daily);p.setInt(3,row.id());p.executeUpdate();}
            atualizarEstadoQuarto(c,room);
            c.commit();mensagem.setText("Check-out realizado. Quarto liberado.");carregarReservas();carregarMapa();
        }catch(Exception e){c.rollback();throw new RuntimeException(e);}}catch(Exception e){mensagem.setText(e.getMessage());}
    }

    private record OpcaoHospede(int id,String nome){@Override public String toString(){return nome;}}
    private record LinhaReserva(int id,int hospedeId,String hospede,String quarto,String entrada,String saida,String status){}
}
