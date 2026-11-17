package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReservationRepository {
    @Value("${hotel.database.url}") private String databaseUrl;
    private Connection connection() throws SQLException { return DriverManager.getConnection(databaseUrl); }

    public List<RoomCard> quartos(LocalDate entrada, LocalDate saida) throws SQLException {
        String sql = "SELECT q.id, q.numero, q.tipo, q.status, q.valor_diaria, q.capacidade, q.descricao, " +
                "NOT EXISTS (SELECT 1 FROM reserva r WHERE r.quarto_id=q.id " +
                "AND r.status NOT IN ('CANCELADA','CONCLUIDA') AND r.data_entrada < ? AND r.data_saida > ?) AS livre " +
                "FROM quarto q ORDER BY q.numero";
        List<RoomCard> result = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, saida.toString()); p.setString(2, entrada.toString());
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) result.add(new RoomCard(r.getInt("id"), r.getString("numero"), r.getString("tipo"),
                        r.getString("status"), r.getDouble("valor_diaria"), r.getInt("capacidade"), r.getString("descricao"),
                        r.getInt("livre") == 1 && !"MANUTENCAO".equals(r.getString("status")) && r.getDouble("valor_diaria") > 0));
            }
        }
        return result;
    }

    public List<GuestOption> hospedes() throws SQLException {
        List<GuestOption> result = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT id,nome FROM hospede WHERE ativo=1 ORDER BY nome"); ResultSet r = p.executeQuery()) {
            while (r.next()) result.add(new GuestOption(r.getInt("id"), r.getString("nome")));
        }
        return result;
    }

    public List<ReservationRow> reservas(Integer hospedeId) throws SQLException {
        String sql = "SELECT r.id,r.hospede_id,r.quarto_id,h.nome,q.numero,r.data_entrada,r.data_saida,r.status " +
                ",g.checkin_em,g.checkout_em,g.valor_total FROM reserva r JOIN hospede h ON h.id=r.hospede_id JOIN quarto q ON q.id=r.quarto_id LEFT JOIN hospedagem g ON g.reserva_id=r.id " +
                (hospedeId == null ? "" : "WHERE r.hospede_id=? ") + "ORDER BY r.data_entrada DESC,r.id DESC";
        List<ReservationRow> result = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            if (hospedeId != null) p.setInt(1, hospedeId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) result.add(new ReservationRow(r.getInt("id"), r.getInt("hospede_id"), r.getInt("quarto_id"), r.getString("nome"),
                        r.getString("numero"), LocalDate.parse(r.getString("data_entrada")), LocalDate.parse(r.getString("data_saida")), r.getString("status"),r.getString("checkin_em"),r.getString("checkout_em"),r.getDouble("valor_total")));
            }
        }
        return result;
    }

    public ReservationRow reserva(int id, Integer donoId) throws SQLException {
        String sql = "SELECT r.id,r.hospede_id,r.quarto_id,h.nome,q.numero,r.data_entrada,r.data_saida,r.status,g.checkin_em,g.checkout_em,g.valor_total FROM reserva r JOIN hospede h ON h.id=r.hospede_id JOIN quarto q ON q.id=r.quarto_id LEFT JOIN hospedagem g ON g.reserva_id=r.id WHERE r.id=?" + (donoId == null ? "" : " AND r.hospede_id=?");
        try (Connection c=connection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,id); if (donoId != null) p.setInt(2,donoId);
            try (ResultSet r=p.executeQuery()) { return r.next() ? new ReservationRow(r.getInt("id"),r.getInt("hospede_id"),r.getInt("quarto_id"),r.getString("nome"),r.getString("numero"),LocalDate.parse(r.getString("data_entrada")),LocalDate.parse(r.getString("data_saida")),r.getString("status"),r.getString("checkin_em"),r.getString("checkout_em"),r.getDouble("valor_total")) : null; }
        }
    }

    public int hospedeId(String login) throws SQLException {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT h.id FROM hospede h JOIN usuario u ON u.id=h.usuario_id WHERE u.login=?")) {
            p.setString(1, login); try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : -1; }
        }
    }

    public void criar(int hospedeId, int quartoId, LocalDate entrada, LocalDate saida) throws SQLException {
        validarPeriodo(entrada, saida);
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                if(!hospedeAtivo(c,hospedeId))throw new IllegalArgumentException("Este hóspede não está ativo no cadastro.");
                if (!disponivel(c, quartoId, entrada, saida, null)) throw new IllegalStateException("Esse quarto não está disponível nesse período.");
                if(temReservaSobreposta(c,hospedeId,entrada,saida,null))throw new IllegalStateException("Este hóspede já tem outra reserva ativa nesse período. Um hóspede pode reservar apenas um quarto por vez.");
                try (PreparedStatement p = c.prepareStatement("INSERT INTO reserva(hospede_id,quarto_id,data_entrada,data_saida,status) VALUES(?,?,?,?, 'PENDENTE')")) {
                    p.setInt(1, hospedeId); p.setInt(2, quartoId); p.setString(3, entrada.toString()); p.setString(4, saida.toString()); p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) { c.rollback(); if (e instanceof SQLException se) throw se; if (e instanceof IllegalStateException ie) throw ie; throw new SQLException(e); }
        }
    }

    public void atualizar(int reservaId, Integer donoId, int hospedeId, int quartoId, LocalDate entrada, LocalDate saida) throws SQLException {
        validarPeriodo(entrada, saida);
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                String sql = "SELECT status,hospede_id FROM reserva WHERE id=?";
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setInt(1, reservaId); try (ResultSet r = p.executeQuery()) {
                        if (!r.next() || (donoId != null && r.getInt("hospede_id") != donoId)) throw new IllegalArgumentException("Reserva não encontrada para esta conta.");
                        if (!List.of("PENDENTE", "CONFIRMADA").contains(r.getString("status"))) throw new IllegalStateException("Só é possível alterar reservas pendentes ou confirmadas.");
                    }
                }
                if (!disponivel(c, quartoId, entrada, saida, reservaId)) throw new IllegalStateException("Esse quarto não está disponível nesse período.");
                if(!hospedeAtivo(c,hospedeId))throw new IllegalArgumentException("Este hóspede não está ativo no cadastro.");
                if(temReservaSobreposta(c,hospedeId,entrada,saida,reservaId))throw new IllegalStateException("Este hóspede já tem outra reserva ativa nesse período. Um hóspede pode reservar apenas um quarto por vez.");
                try (PreparedStatement p = c.prepareStatement("UPDATE reserva SET hospede_id=?,quarto_id=?,data_entrada=?,data_saida=? WHERE id=?")) {
                    p.setInt(1,hospedeId); p.setInt(2,quartoId); p.setString(3,entrada.toString()); p.setString(4,saida.toString()); p.setInt(5,reservaId); p.executeUpdate();
                }
                c.commit();
            } catch (Exception e) { c.rollback(); if (e instanceof SQLException se) throw se; if (e instanceof IllegalArgumentException ia) throw ia; if (e instanceof IllegalStateException ie) throw ie; throw new SQLException(e); }
        }
    }

    public void cancelar(int reservaId, Integer donoId) throws SQLException {
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try{
                int quartoId;
                String sql="SELECT quarto_id FROM reserva WHERE id=? AND status IN ('PENDENTE','CONFIRMADA')"+(donoId==null?"":" AND hospede_id=?");
                try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,reservaId);if(donoId!=null)p.setInt(2,donoId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Não foi possível cancelar: confira se a reserva ainda pode ser cancelada.");quartoId=r.getInt(1);}}
                try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='CANCELADA' WHERE id=?")){p.setInt(1,reservaId);p.executeUpdate();}
                atualizarDisponibilidadeQuarto(c,quartoId);
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalStateException ie)throw ie;throw new SQLException(e);}
        }
    }

    public int excluirCanceladas()throws SQLException{
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try(Statement stmt=c.createStatement()){
                List<Integer> quartosAfetados=new ArrayList<>();
                try(ResultSet r=stmt.executeQuery("SELECT DISTINCT quarto_id FROM reserva WHERE status='CANCELADA'")){while(r.next())quartosAfetados.add(r.getInt(1));}
                for(int quartoId:quartosAfetados)atualizarDisponibilidadeQuarto(c,quartoId);
                stmt.executeUpdate("DELETE FROM hospedagem WHERE reserva_id IN (SELECT id FROM reserva WHERE status='CANCELADA')");
                int removidas=stmt.executeUpdate("DELETE FROM reserva WHERE status='CANCELADA'");
                c.commit();return removidas;
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException(e);}
        }
    }

    public void confirmar(int id)throws SQLException{mudarStatus(id,"PENDENTE","CONFIRMADA");}

    public void checkin(int id)throws SQLException{
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try{
                int quartoId;LocalDate entrada;LocalDate saida;String quartoStatus;
                try(PreparedStatement p=c.prepareStatement("SELECT r.quarto_id,r.data_entrada,r.data_saida,r.status,q.status AS quarto_status FROM reserva r JOIN quarto q ON q.id=r.quarto_id WHERE r.id=?")){
                    p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                        if(!r.next()||!"CONFIRMADA".equals(r.getString("status")))throw new IllegalStateException("O check-in exige uma reserva confirmada.");
                        quartoId=r.getInt("quarto_id");entrada=LocalDate.parse(r.getString("data_entrada"));saida=LocalDate.parse(r.getString("data_saida"));quartoStatus=r.getString("quarto_status");
                    }
                }
                LocalDate hoje=LocalDate.now();if(hoje.isBefore(entrada)||!hoje.isBefore(saida))throw new IllegalStateException("O check-in só pode ocorrer dentro do período reservado.");
                if("MANUTENCAO".equals(quartoStatus)||temHospedagemAtiva(c,quartoId))throw new IllegalStateException("O quarto não está disponível para check-in.");
                try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='EM_HOSPEDAGEM' WHERE id=? AND status='CONFIRMADA'")){p.setInt(1,id);if(p.executeUpdate()==0)throw new IllegalStateException("A reserva não pode receber check-in.");}
                try(PreparedStatement p=c.prepareStatement("UPDATE quarto SET status='OCUPADO' WHERE id=?")){p.setInt(1,quartoId);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("INSERT INTO hospedagem(reserva_id,checkin_em,valor_total) VALUES(?,?,0)")){p.setInt(1,id);p.setString(2,java.time.LocalDateTime.now().toString());p.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalStateException ie)throw ie;throw new SQLException(e);}
        }
    }

    public void checkout(int id)throws SQLException{
        try(Connection c=connection()){
            c.setAutoCommit(false);
            try{
                int quartoId;String checkin;
                try(PreparedStatement p=c.prepareStatement("SELECT r.quarto_id,h.checkin_em FROM reserva r JOIN hospedagem h ON h.reserva_id=r.id WHERE r.id=? AND r.status='EM_HOSPEDAGEM' AND h.checkout_em IS NULL")){
                    p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("Só é possível fazer check-out de uma hospedagem ativa.");quartoId=r.getInt("quarto_id");checkin=r.getString("checkin_em");}
                }
                java.time.LocalDate hoje=java.time.LocalDate.now();java.time.LocalDate diaEntrada=java.time.LocalDate.parse(checkin.substring(0,10));long noites=Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(diaEntrada,hoje));
                double diaria;try(PreparedStatement p=c.prepareStatement("SELECT q.valor_diaria FROM quarto q WHERE q.id=?")){p.setInt(1,quartoId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Quarto não encontrado.");diaria=r.getDouble(1);}}
                try(PreparedStatement p=c.prepareStatement("UPDATE reserva SET status='CONCLUIDA' WHERE id=? AND status='EM_HOSPEDAGEM'")){p.setInt(1,id);if(p.executeUpdate()==0)throw new IllegalStateException("A hospedagem já foi encerrada.");}
                try(PreparedStatement p=c.prepareStatement("UPDATE hospedagem SET checkout_em=?,valor_total=? WHERE reserva_id=? AND checkout_em IS NULL")){p.setString(1,java.time.LocalDateTime.now().toString());p.setDouble(2,noites*diaria);p.setInt(3,id);p.executeUpdate();}
                atualizarDisponibilidadeQuarto(c,quartoId);
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;if(e instanceof IllegalStateException ie)throw ie;throw new SQLException(e);}
        }
    }

    private void mudarStatus(int id,String esperado,String novo)throws SQLException{
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("UPDATE reserva SET status=? WHERE id=? AND status=?")){p.setString(1,novo);p.setInt(2,id);p.setString(3,esperado);if(p.executeUpdate()==0)throw new IllegalStateException("A reserva não está no estado necessário para esta ação.");}
    }

    private boolean disponivel(Connection c, int quartoId, LocalDate entrada, LocalDate saida, Integer ignorarId) throws SQLException {
        String sql = "SELECT q.status,q.valor_diaria, NOT EXISTS (SELECT 1 FROM reserva r WHERE r.quarto_id=q.id AND r.status NOT IN ('CANCELADA','CONCLUIDA') AND r.data_entrada < ? AND r.data_saida > ?" + (ignorarId == null ? "" : " AND r.id<>?") + ") AS livre FROM quarto q WHERE q.id=?";
        try (PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,saida.toString()); p.setString(2,entrada.toString()); int index=3;
            if (ignorarId != null) p.setInt(index++,ignorarId); p.setInt(index,quartoId);
            try (ResultSet r=p.executeQuery()) { return r.next() && !"MANUTENCAO".equals(r.getString("status")) && r.getDouble("valor_diaria")>0 && r.getInt("livre")==1; }
        }
    }
    private boolean hospedeAtivo(Connection c,int hospedeId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM hospede WHERE id=? AND ativo=1")){p.setInt(1,hospedeId);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private void atualizarDisponibilidadeQuarto(Connection c,int quartoId)throws SQLException{
        boolean ocupado;
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,quartoId);try(ResultSet r=p.executeQuery()){ocupado=r.next();}}
        try(PreparedStatement p=c.prepareStatement("UPDATE quarto SET status=? WHERE id=? AND status<>'MANUTENCAO'")){p.setString(1,ocupado?"OCUPADO":"DISPONIVEL");p.setInt(2,quartoId);p.executeUpdate();}
    }
    private boolean temHospedagemAtiva(Connection c,int quartoId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status='EM_HOSPEDAGEM' LIMIT 1")){p.setInt(1,quartoId);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private boolean temReservaSobreposta(Connection c,int hospedeId,LocalDate entrada,LocalDate saida,Integer ignorarId)throws SQLException{
        String sql="SELECT 1 FROM reserva WHERE hospede_id=? AND status NOT IN ('CANCELADA','CONCLUIDA') AND data_entrada < ? AND data_saida > ?"+(ignorarId==null?"":" AND id<>?")+" LIMIT 1";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,hospedeId);p.setString(2,saida.toString());p.setString(3,entrada.toString());if(ignorarId!=null)p.setInt(4,ignorarId);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private void validarPeriodo(LocalDate entrada, LocalDate saida) {
        if (entrada == null || saida == null || entrada.isBefore(LocalDate.now()) || !saida.isAfter(entrada)) throw new IllegalArgumentException("Escolha uma entrada a partir de hoje e uma saída depois da entrada.");
    }

    public record RoomCard(int id, String numero, String tipo, String status, double valorDiaria, int capacidade, String descricao, boolean disponivel) {}
    public record GuestOption(int id, String nome) {}
    public record ReservationRow(int id, int hospedeId, int quartoId, String hospede, String quarto, LocalDate entrada, LocalDate saida, String status,String checkinEm,String checkoutEm,double valorTotal) {}
}
