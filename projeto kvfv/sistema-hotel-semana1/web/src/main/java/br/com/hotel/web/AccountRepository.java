package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AccountRepository {
    @Value("${hotel.database.url}") private String databaseUrl;
    private Connection connection() throws Exception { return DriverManager.getConnection(databaseUrl); }

    public void cadastrarHospede(String nome, String login, String senha, PasswordEncoder encoder) throws Exception {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                int usuarioId;
                try (PreparedStatement p = c.prepareStatement("INSERT INTO usuario (nome, login, senha, perfil) VALUES (?, ?, ?, 'HOSPEDE')")) {
                    p.setString(1, nome.trim()); p.setString(2, login.trim().toLowerCase()); p.setString(3, encoder.encode(senha)); p.executeUpdate();
                    try (var keyQuery = c.createStatement(); ResultSet keys = keyQuery.executeQuery("SELECT last_insert_rowid()")) { if (!keys.next()) throw new IllegalStateException("Não foi possível criar a conta."); usuarioId = keys.getInt(1); }
                }
                Integer hospedeExistente = null;
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM hospede WHERE lower(email) = ? AND usuario_id IS NULL AND ativo=1 LIMIT 1")) {
                    p.setString(1, login.trim().toLowerCase());
                    try (ResultSet r = p.executeQuery()) { if (r.next()) hospedeExistente = r.getInt("id"); }
                }
                if (hospedeExistente == null) {
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO hospede (nome, email, usuario_id) VALUES (?, ?, ?)")) { p.setString(1, nome.trim()); p.setString(2, login.trim().toLowerCase()); p.setInt(3, usuarioId); p.executeUpdate(); }
                } else {
                    try (PreparedStatement p = c.prepareStatement("UPDATE hospede SET usuario_id = ? WHERE id = ? AND usuario_id IS NULL")) { p.setInt(1, usuarioId); p.setInt(2, hospedeExistente); p.executeUpdate(); }
                }
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    public Hospede hospedeDaConta(String login) throws Exception {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT h.id, h.nome, h.documento, h.telefone, h.email FROM hospede h JOIN usuario u ON h.usuario_id = u.id WHERE u.login = ?")) {
            p.setString(1, login);
            try (ResultSet r = p.executeQuery()) { return r.next() ? new Hospede(r.getInt("id"), r.getString("nome"), r.getString("documento"), r.getString("telefone"), r.getString("email")) : null; }
        }
    }

    public void cadastrarRecepcionista(String nome, String login, String senha, PasswordEncoder encoder) throws Exception {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO usuario (nome, login, senha, perfil) VALUES (?, ?, ?, 'RECEPCIONISTA')")) {
            p.setString(1, nome.trim()); p.setString(2, login.trim().toLowerCase()); p.setString(3, encoder.encode(senha)); p.executeUpdate();
        }
    }

    public List<StaffAccount> funcionarios() throws Exception {
        List<StaffAccount> list=new ArrayList<>();
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id,nome,login FROM usuario WHERE perfil='RECEPCIONISTA' ORDER BY nome");ResultSet r=p.executeQuery()){
            while(r.next())list.add(new StaffAccount(r.getInt("id"),r.getString("nome"),r.getString("login")));
        }
        return list;
    }

    public StaffAccount funcionario(int id) throws Exception {
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id,nome,login FROM usuario WHERE id=? AND perfil='RECEPCIONISTA'")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?new StaffAccount(r.getInt("id"),r.getString("nome"),r.getString("login")):null;}
        }
    }

    public void atualizarFuncionario(int id,String nome,String login,String novaSenha,PasswordEncoder encoder)throws Exception{
        if(nome==null||nome.isBlank()||login==null||login.isBlank())throw new IllegalArgumentException("Informe nome e e-mail do funcionário.");
        try(Connection c=connection()){
            String sql=novaSenha==null||novaSenha.isBlank()?"UPDATE usuario SET nome=?,login=? WHERE id=? AND perfil='RECEPCIONISTA'":"UPDATE usuario SET nome=?,login=?,senha=? WHERE id=? AND perfil='RECEPCIONISTA'";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,nome.trim());p.setString(2,login.trim().toLowerCase(java.util.Locale.ROOT));int idx=3;if(novaSenha!=null&&!novaSenha.isBlank())p.setString(idx++,encoder.encode(novaSenha));p.setInt(idx,id);if(p.executeUpdate()==0)throw new IllegalArgumentException("Funcionário não encontrado.");}
        }
    }

    public void excluirFuncionario(int id)throws Exception{
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("DELETE FROM usuario WHERE id=? AND perfil='RECEPCIONISTA'")){p.setInt(1,id);if(p.executeUpdate()==0)throw new IllegalArgumentException("Funcionário não encontrado.");}
    }

    public void atualizarDadosHospede(String login,String nome,String novoLogin,String documento,String telefone)throws Exception{
        if(nome==null||nome.isBlank()||novoLogin==null||novoLogin.isBlank())throw new IllegalArgumentException("Informe nome e e-mail.");
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try{
                int id;
                try(PreparedStatement p=c.prepareStatement("SELECT id FROM usuario WHERE login=? AND perfil='HOSPEDE'")){p.setString(1,login);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Conta de hóspede não encontrada.");id=r.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("UPDATE usuario SET nome=?,login=? WHERE id=?")){p.setString(1,nome.trim());p.setString(2,novoLogin.trim().toLowerCase(java.util.Locale.ROOT));p.setInt(3,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("UPDATE hospede SET nome=?,email=?,documento=?,telefone=? WHERE usuario_id=?")){p.setString(1,nome.trim());p.setString(2,novoLogin.trim().toLowerCase(java.util.Locale.ROOT));p.setString(3,vazio(documento));p.setString(4,vazio(telefone));p.setInt(5,id);p.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    public void alterarSenhaHospede(String login,String atual,String nova,PasswordEncoder encoder)throws Exception{
        if(nova==null||nova.length()<6)throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");
        int usuarioId;
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id,senha FROM usuario WHERE login=? AND perfil='HOSPEDE'")){
            p.setString(1,login);try(ResultSet r=p.executeQuery()){
                if(!r.next()||!encoder.matches(atual,r.getString("senha")))throw new IllegalArgumentException("A senha atual está incorreta.");
                usuarioId=r.getInt("id");
            }
        }
        try(Connection c=connection();PreparedStatement update=c.prepareStatement("UPDATE usuario SET senha=? WHERE id=? AND perfil='HOSPEDE'")){
            update.setString(1,encoder.encode(nova));update.setInt(2,usuarioId);
            if(update.executeUpdate()==0)throw new IllegalArgumentException("Conta de hóspede não encontrada.");
        }
    }

    private String vazio(String value){return value==null||value.isBlank()?null:value.trim();}

    public record StaffAccount(int id,String nome,String login){}
}
