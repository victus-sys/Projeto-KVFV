package br.com.hotel;

import br.com.hotel.domain.Usuario;
import br.com.hotel.security.PasswordHasher;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Ferramentas administrativas de quartos, acessos de recepção e relatórios básicos. */
public final class TelaAdmin {
    private TelaAdmin() {}

    public static void mostrar(Stage stage, Usuario usuario, Runnable voltar) {
        TabPane tabs=new TabPane();
        tabs.getTabs().add(new Tab("Quartos",new RoomsPane()));
        tabs.getTabs().add(new Tab("Funcionários",new StaffPane()));
        tabs.getTabs().add(new Tab("Relatórios",new ReportsPane()));
        tabs.getTabs().forEach(t->t.setClosable(false));
        Button back=new Button("Voltar ao atendimento");back.setOnAction(e->voltar.run());
        VBox layout=new VBox(14,back,tabs);layout.setPadding(new Insets(18));VBox.setVgrow(tabs,Priority.ALWAYS);
        stage.setTitle("Hotel Alba - Administração");stage.setScene(new Scene(layout,1100,740));stage.show();
    }

    private static final class RoomsPane extends VBox {
        private final TableView<RoomRow> table=new TableView<>();
        private final TextField number=new TextField(),rate=new TextField(),capacity=new TextField(),description=new TextField();
        private final ComboBox<String> type=new ComboBox<>(),status=new ComboBox<>();
        private final Label message=new Label();private Integer editingId;
        RoomsPane(){
            setSpacing(10);setPadding(new Insets(12));number.setPromptText("Número do quarto");rate.setPromptText("Valor da diária (R$)");capacity.setPromptText("Capacidade");description.setPromptText("Descrição");
            type.getItems().setAll("SOLTEIRO","CASAL","FAMILIA","SUITE");type.setPromptText("Tipo");status.getItems().setAll("DISPONIVEL","OCUPADO","MANUTENCAO");status.setPromptText("Situação");
            configureTable();HBox top=new HBox(8,number,type,status,rate,capacity);HBox.setHgrow(number,Priority.ALWAYS);HBox.setHgrow(rate,Priority.ALWAYS);
            HBox bottom=new HBox(8,description,new Button("Salvar quarto"),new Button("Limpar"),new Button("Excluir selecionado"));
            Button save=(Button)bottom.getChildren().get(1),clear=(Button)bottom.getChildren().get(2),delete=(Button)bottom.getChildren().get(3);HBox.setHgrow(description,Priority.ALWAYS);
            save.setOnAction(e->save());clear.setOnAction(e->clear());delete.setOnAction(e->delete());table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null)load(b);});
            getChildren().addAll(new Label("Cadastre o inventário real de quartos; nenhum quarto é criado automaticamente."),top,bottom,message,table);VBox.setVgrow(table,Priority.ALWAYS);refresh();
        }
        private void configureTable(){
            TableColumn<RoomRow,String> n=new TableColumn<>("Quarto"),t=new TableColumn<>("Tipo"),s=new TableColumn<>("Situação"),v=new TableColumn<>("Diária"),c=new TableColumn<>("Capacidade");
            n.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().numero));t.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().tipo));s.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().status));v.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(String.format("R$ %.2f",d.getValue().valor)));c.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(String.valueOf(d.getValue().capacidade)));
            table.getColumns().setAll(n,t,s,v,c);table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
        private void refresh(){List<RoomRow> rows=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,numero,tipo,status,valor_diaria,capacidade,descricao FROM quarto ORDER BY numero");ResultSet r=p.executeQuery()){while(r.next())rows.add(new RoomRow(r.getInt("id"),r.getString("numero"),r.getString("tipo"),r.getString("status"),r.getDouble("valor_diaria"),r.getInt("capacidade"),r.getString("descricao")));table.setItems(FXCollections.observableArrayList(rows));}catch(SQLException e){message.setText("Erro ao carregar quartos: "+e.getMessage());}}
        private void load(RoomRow row){editingId=row.id;number.setText(row.numero);type.setValue(row.tipo);status.setValue(row.status);rate.setText(String.valueOf(row.valor));capacity.setText(String.valueOf(row.capacidade));description.setText(row.descricao);}
        private void clear(){editingId=null;table.getSelectionModel().clearSelection();number.clear();type.setValue(null);status.setValue(null);rate.clear();capacity.clear();description.clear();message.setText("Formulário limpo.");}
        private void save(){
            try{
                if(number.getText().isBlank()||type.getValue()==null||status.getValue()==null)throw new IllegalArgumentException("Preencha número, tipo e situação.");
                double daily=Double.parseDouble(rate.getText().replace(',','.'));int people=Integer.parseInt(capacity.getText());if(daily<=0||people<1)throw new IllegalArgumentException("Informe diária positiva e capacidade de pelo menos uma pessoa.");
                try(Connection c=Database.getConnection()){
                    c.setAutoCommit(false);
                    try{
                        if(editingId!=null){String old;try(PreparedStatement q=c.prepareStatement("SELECT status FROM quarto WHERE id=?")){q.setInt(1,editingId);try(ResultSet r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("Quarto não encontrado.");old=r.getString(1);}}
                            if(!old.equals(status.getValue())&&!"DISPONIVEL".equals(status.getValue())&&hasActiveReservations(c,editingId))throw new IllegalStateException("Há reservas ativas; faça a mudança pelo fluxo de hospedagem.");}
                        String sql=editingId==null?"INSERT INTO quarto(numero,tipo,status,valor_diaria,capacidade,descricao) VALUES(?,?,?,?,?,?)":"UPDATE quarto SET numero=?,tipo=?,status=?,valor_diaria=?,capacidade=?,descricao=? WHERE id=?";
                        try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,number.getText().trim());p.setString(2,type.getValue());p.setString(3,status.getValue());p.setDouble(4,daily);p.setInt(5,people);p.setString(6,description.getText().isBlank()?null:description.getText().trim());if(editingId!=null)p.setInt(7,editingId);p.executeUpdate();}
                        c.commit();message.setText(editingId==null?"Quarto cadastrado.":"Quarto atualizado.");
                    }catch(Exception e){c.rollback();throw e;}
                }
                clear();refresh();
            }catch(Exception e){message.setText(e.getMessage());}
        }
        private boolean hasActiveReservations(Connection c,int id)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status IN ('PENDENTE','CONFIRMADA','EM_HOSPEDAGEM') LIMIT 1")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next();}}}
        private void delete(){RoomRow row=table.getSelectionModel().getSelectedItem();if(row==null){message.setText("Selecione um quarto.");return;}try(Connection c=Database.getConnection()){try(PreparedStatement check=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? LIMIT 1")){check.setInt(1,row.id);try(ResultSet r=check.executeQuery()){if(r.next())throw new IllegalStateException("Não exclua um quarto com histórico de reservas.");}}try(PreparedStatement p=c.prepareStatement("DELETE FROM quarto WHERE id=?")){p.setInt(1,row.id);p.executeUpdate();}clear();refresh();message.setText("Quarto excluído.");}catch(Exception e){message.setText(e.getMessage());}}
    }

    private static final class StaffPane extends VBox {
        private final TableView<StaffRow> table=new TableView<>();private final TextField name=new TextField(),login=new TextField();private final PasswordField password=new PasswordField();private final Label message=new Label();private Integer editingId;
        StaffPane(){setSpacing(10);setPadding(new Insets(12));name.setPromptText("Nome completo");login.setPromptText("E-mail de acesso");password.setPromptText("Senha (mínimo 6 caracteres)");configure();HBox form=new HBox(8,name,login,password);HBox.setHgrow(name,Priority.ALWAYS);HBox.setHgrow(login,Priority.ALWAYS);Button save=new Button("Criar acesso / salvar alterações"),clear=new Button("Novo funcionário"),delete=new Button("Excluir selecionado");save.setOnAction(e->save());clear.setOnAction(e->clear());delete.setOnAction(e->delete());table.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null)load(b);});getChildren().addAll(new Label("Os funcionários recebem acesso de RECEPCIONISTA; a conta de administrador não pode ser alterada aqui."),form,new HBox(8,save,clear,delete),message,table);VBox.setVgrow(table,Priority.ALWAYS);refresh();}
        private void configure(){TableColumn<StaffRow,String> n=new TableColumn<>("Nome"),l=new TableColumn<>("Login");n.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().nome));l.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().login));table.getColumns().setAll(n,l);table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);}
        private void refresh(){List<StaffRow> rows=new ArrayList<>();try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,nome,login FROM usuario WHERE perfil='RECEPCIONISTA' ORDER BY nome");ResultSet r=p.executeQuery()){while(r.next())rows.add(new StaffRow(r.getInt(1),r.getString(2),r.getString(3)));table.setItems(FXCollections.observableArrayList(rows));}catch(SQLException e){message.setText(e.getMessage());}}
        private void load(StaffRow row){editingId=row.id;name.setText(row.nome);login.setText(row.login);password.clear();password.setPromptText("Nova senha (opcional)");}
        private void clear(){editingId=null;table.getSelectionModel().clearSelection();name.clear();login.clear();password.clear();password.setPromptText("Senha (mínimo 6 caracteres)");}
        private void save(){try{if(name.getText().isBlank()||login.getText().isBlank()||!login.getText().contains("@"))throw new IllegalArgumentException("Informe nome e e-mail válidos.");if(editingId==null&&password.getText().length()<6)throw new IllegalArgumentException("A senha inicial deve ter pelo menos 6 caracteres.");if(!password.getText().isBlank()&&password.getText().length()<6)throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");String sql=editingId==null?"INSERT INTO usuario(nome,login,senha,perfil) VALUES(?,?,?,'RECEPCIONISTA')":(password.getText().isBlank()?"UPDATE usuario SET nome=?,login=? WHERE id=? AND perfil='RECEPCIONISTA'":"UPDATE usuario SET nome=?,login=?,senha=? WHERE id=? AND perfil='RECEPCIONISTA'");try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setString(1,name.getText().trim());p.setString(2,login.getText().trim().toLowerCase(java.util.Locale.ROOT));int ix=3;if(editingId==null||!password.getText().isBlank())p.setString(ix++,PasswordHasher.hash(password.getText()));if(editingId!=null)p.setInt(ix,editingId);p.executeUpdate();}message.setText(editingId==null?"Acesso criado.":"Funcionário atualizado.");clear();refresh();}catch(Exception e){message.setText("Não foi possível salvar: "+e.getMessage());}}
        private void delete(){StaffRow row=table.getSelectionModel().getSelectedItem();if(row==null){message.setText("Selecione um funcionário.");return;}try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM usuario WHERE id=? AND perfil='RECEPCIONISTA'")){p.setInt(1,row.id);if(p.executeUpdate()==0)throw new IllegalStateException("Funcionário não encontrado.");clear();refresh();message.setText("Acesso excluído.");}catch(Exception e){message.setText(e.getMessage());}}
    }

    private static final class ReportsPane extends VBox {
        ReportsPane(){setSpacing(12);setPadding(new Insets(18));getChildren().add(new Label("Resumo do hotel"));try(Connection c=Database.getConnection();Statement s=c.createStatement()){
            addMetric(s,"Hóspedes cadastrados","SELECT COUNT(*) FROM hospede WHERE ativo=1");addMetric(s,"Quartos cadastrados","SELECT COUNT(*) FROM quarto");addMetric(s,"Quartos disponíveis","SELECT COUNT(*) FROM quarto WHERE status='DISPONIVEL' AND valor_diaria>0");addMetric(s,"Quartos ocupados","SELECT COUNT(*) FROM quarto WHERE status='OCUPADO'");addMetric(s,"Quartos em manutenção","SELECT COUNT(*) FROM quarto WHERE status='MANUTENCAO'");addMetric(s,"Reservas pendentes","SELECT COUNT(*) FROM reserva WHERE status='PENDENTE'");addMetric(s,"Reservas confirmadas","SELECT COUNT(*) FROM reserva WHERE status='CONFIRMADA'");addMetric(s,"Hospedagens ativas","SELECT COUNT(*) FROM reserva WHERE status='EM_HOSPEDAGEM'");
        }catch(SQLException e){getChildren().add(new Label("Erro ao consultar relatórios: "+e.getMessage()));}}
        private void addMetric(Statement s,String label,String sql)throws SQLException{try(ResultSet r=s.executeQuery(sql)){r.next();getChildren().add(new Label(label+": "+r.getInt(1)));}}
    }
    private record RoomRow(int id,String numero,String tipo,String status,double valor,int capacidade,String descricao){}
    private record StaffRow(int id,String nome,String login){}
}
