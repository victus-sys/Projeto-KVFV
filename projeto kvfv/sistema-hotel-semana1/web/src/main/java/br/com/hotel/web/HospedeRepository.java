package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HospedeRepository {
    @Value("${hotel.database.url}") private String databaseUrl;

    private Connection connection() throws SQLException { return DriverManager.getConnection(databaseUrl); }

    public List<Hospede> buscar(String termo) throws SQLException {
        String busca = "%" + (termo == null ? "" : termo.trim()) + "%";
        List<Hospede> hospedes = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "SELECT id, nome, documento, telefone, email FROM hospede WHERE ativo=1 AND (nome LIKE ? OR documento LIKE ?) ORDER BY id DESC")) {
            p.setString(1, busca); p.setString(2, busca);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) hospedes.add(new Hospede(r.getInt("id"), r.getString("nome"), r.getString("documento"), r.getString("telefone"), r.getString("email")));
            }
        }
        return hospedes;
    }

    public List<Hospede> buscarInativos()throws SQLException{
        List<Hospede> hospedes=new ArrayList<>();
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id,nome,documento,telefone,email FROM hospede WHERE ativo=0 ORDER BY id DESC");ResultSet r=p.executeQuery()){
            while(r.next())hospedes.add(new Hospede(r.getInt("id"),r.getString("nome"),r.getString("documento"),r.getString("telefone"),r.getString("email")));
        }
        return hospedes;
    }

    public Hospede buscarPorId(int id) throws SQLException {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT id, nome, documento, telefone, email FROM hospede WHERE id = ? AND ativo=1")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Hospede(r.getInt("id"), r.getString("nome"), r.getString("documento"), r.getString("telefone"), r.getString("email")) : null;
            }
        }
    }

    public int salvar(Integer id, HospedeForm form) throws SQLException {
        try (Connection c = connection()) {
            if(id==null){
                try(PreparedStatement p=c.prepareStatement("INSERT INTO hospede (nome,documento,telefone,email) VALUES (?,?,?,?)")){
                    preencher(p,form);p.executeUpdate();
                }
                try(PreparedStatement lastId=c.prepareStatement("SELECT last_insert_rowid()");ResultSet r=lastId.executeQuery()){return r.next()?r.getInt(1):-1;}
            }
            c.setAutoCommit(false);
            try{
                Integer usuarioId=null;
                try(PreparedStatement p=c.prepareStatement("SELECT usuario_id FROM hospede WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóspede não encontrado.");int value=r.getInt(1);if(!r.wasNull())usuarioId=value;}}
                if(usuarioId!=null){
                    if(form.getEmail()==null||form.getEmail().isBlank())throw new IllegalArgumentException("O e-mail de acesso é obrigatório para uma conta vinculada.");
                    try(PreparedStatement p=c.prepareStatement("UPDATE usuario SET nome=?,login=? WHERE id=?")){p.setString(1,form.getNome().trim());p.setString(2,form.getEmail().trim().toLowerCase(java.util.Locale.ROOT));p.setInt(3,usuarioId);p.executeUpdate();}
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE hospede SET nome=?,documento=?,telefone=?,email=? WHERE id=?")){p.setString(1,form.getNome().trim());p.setString(2,vazioNulo(form.getDocumento()));p.setString(3,vazioNulo(form.getTelefone()));p.setString(4,vazioNulo(form.getEmail()));p.setInt(5,id);p.executeUpdate();}
                c.commit();return id;
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalArgumentException ia)throw ia;throw new SQLException(e);}
        }
    }

    public int excluir(int id) throws SQLException {
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try{
                Integer usuarioId=null;try(PreparedStatement p=c.prepareStatement("SELECT usuario_id FROM hospede WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Hóspede não encontrado.");int value=r.getInt(1);if(!r.wasNull())usuarioId=value;}}
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE hospede_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalStateException("Não é possível excluir um hóspede que está hospedado agora.");}}
                int reservas;
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM reserva WHERE hospede_id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();reservas=r.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM hospedagem WHERE reserva_id IN (SELECT id FROM reserva WHERE hospede_id=?)")){p.setInt(1,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM reserva WHERE hospede_id=?")){p.setInt(1,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM hospede WHERE id=?")){p.setInt(1,id);p.executeUpdate();}
                if(usuarioId!=null)try(PreparedStatement p=c.prepareStatement("DELETE FROM usuario WHERE id=? AND perfil='HOSPEDE'")){p.setInt(1,usuarioId);p.executeUpdate();}
                c.commit();return reservas;
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalArgumentException ia)throw ia;if(e instanceof IllegalStateException ie)throw ie;throw new SQLException(e);}
        }
    }

    private void preencher(PreparedStatement p,HospedeForm f)throws SQLException{p.setString(1,f.getNome().trim());p.setString(2,vazioNulo(f.getDocumento()));p.setString(3,vazioNulo(f.getTelefone()));p.setString(4,vazioNulo(f.getEmail()));}

    private String vazioNulo(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
}
