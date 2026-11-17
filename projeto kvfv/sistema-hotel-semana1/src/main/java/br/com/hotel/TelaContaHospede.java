package br.com.hotel;

import br.com.hotel.domain.Hospede;
import br.com.hotel.domain.Usuario;
import br.com.hotel.security.PasswordHasher;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TelaContaHospede {
    private TelaContaHospede(){}
    public static void mostrar(Stage stage,Usuario usuario,Runnable voltar){
        VBox layout=new VBox(12);layout.setPadding(new Insets(26));Label title=new Label("Meus dados");title.setStyle("-fx-font-size:24px;-fx-font-weight:bold;");Label message=new Label();
        try{
            Hospede h=Database.buscarHospedeDoUsuario(usuario.id());if(h==null){layout.getChildren().add(new Label("Não há cadastro associado a esta conta."));}
            else{
                TextField name=new TextField(h.getNome()),email=new TextField(h.getEmail()),doc=new TextField(h.getDocumento()),phone=new TextField(h.getTelefone());Button save=new Button("Salvar dados pessoais");
                save.setOnAction(e->{try{Database.salvarHospede(h.getId(),name.getText(),doc.getText(),phone.getText(),email.getText());message.setText("Dados atualizados. Se alterou o e-mail, use-o no próximo login.");}catch(Exception ex){message.setText("Não foi possível salvar: "+ex.getMessage());}});
                PasswordField current=new PasswordField(),next=new PasswordField(),confirm=new PasswordField();current.setPromptText("Senha atual");next.setPromptText("Nova senha (mínimo 6 caracteres)");confirm.setPromptText("Confirme a nova senha");Button change=new Button("Alterar senha");
                change.setOnAction(e->{try{if(next.getText().length()<6||!next.getText().equals(confirm.getText()))throw new IllegalArgumentException("Confira a confirmação e use pelo menos 6 caracteres.");try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT senha FROM usuario WHERE id=? AND perfil='HOSPEDE'")){p.setInt(1,usuario.id());try(ResultSet r=p.executeQuery()){if(!r.next()||!PasswordHasher.matches(current.getText(),r.getString(1)))throw new IllegalArgumentException("A senha atual está incorreta.");}}try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE usuario SET senha=? WHERE id=? AND perfil='HOSPEDE'")){p.setString(1,PasswordHasher.hash(next.getText()));p.setInt(2,usuario.id());p.executeUpdate();}message.setText("Senha alterada.");current.clear();next.clear();confirm.clear();}catch(Exception ex){message.setText(ex.getMessage());}});
                layout.getChildren().addAll(new Label("Nome completo"),name,new Label("E-mail de acesso"),email,new Label("Documento"),doc,new Label("Telefone"),phone,save,new Separator(),new Label("Alterar senha"),current,next,confirm,change);
            }
        }catch(SQLException e){message.setText("Não foi possível carregar seus dados: "+e.getMessage());}
        Button back=new Button("Voltar");back.setOnAction(e->voltar.run());layout.getChildren().addAll(message,back);stage.setTitle("Hotel Alba - Minha conta");stage.setScene(new Scene(layout,620,700));stage.show();
    }
}
