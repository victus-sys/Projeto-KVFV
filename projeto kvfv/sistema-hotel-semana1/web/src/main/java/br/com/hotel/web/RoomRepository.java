package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomRepository {
    @Value("${hotel.database.url}") private String databaseUrl;
    private Connection connection() throws SQLException { return DriverManager.getConnection(databaseUrl); }

    public List<Room> listar() throws SQLException { return listarEm(null, null); }
    public Room buscar(int id) throws SQLException {
        try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id,numero,tipo,status,valor_diaria,capacidade,descricao,(status='DISPONIVEL') AS livre FROM quarto WHERE id=?")){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next()?new Room(r.getInt("id"),r.getString("numero"),r.getString("tipo"),r.getString("status"),r.getDouble("valor_diaria"),r.getInt("capacidade"),r.getString("descricao"),r.getInt("livre")==1):null;}
        }
    }
    public List<Room> listarEm(LocalDate entrada, LocalDate saida) throws SQLException {
        boolean porPeriodo=entrada!=null && saida!=null;
        String sql="SELECT q.id,q.numero,q.tipo,q.status,q.valor_diaria,q.capacidade,q.descricao,"+
                (porPeriodo ? "q.status<>'MANUTENCAO' AND NOT EXISTS(SELECT 1 FROM reserva r WHERE r.quarto_id=q.id AND r.status NOT IN ('CANCELADA','CONCLUIDA') AND r.data_entrada < ? AND r.data_saida > ?)" : "q.status='DISPONIVEL'")+" AS livre FROM quarto q ORDER BY q.numero";
        List<Room> result=new ArrayList<>();
        try(Connection c=connection();PreparedStatement p=c.prepareStatement(sql)){
            if(porPeriodo){p.setString(1,saida.toString());p.setString(2,entrada.toString());}
            try(ResultSet r=p.executeQuery()){while(r.next())result.add(new Room(r.getInt("id"),r.getString("numero"),r.getString("tipo"),r.getString("status"),r.getDouble("valor_diaria"),r.getInt("capacidade"),r.getString("descricao"),r.getInt("livre")==1));}
        }
        return result;
    }

    public void salvar(Integer id, RoomForm form) throws SQLException {
        if(form.getNumero()==null || form.getNumero().isBlank())throw new IllegalArgumentException("Informe o número do quarto.");
        if(!List.of("SOLTEIRO","CASAL","FAMILIA","SUITE").contains(form.getTipo()))throw new IllegalArgumentException("Selecione um tipo de quarto.");
        if(!List.of("DISPONIVEL","OCUPADO","MANUTENCAO").contains(form.getStatus()))throw new IllegalArgumentException("Selecione uma situação válida.");
        if(form.getValorDiaria()==null || form.getValorDiaria()<=0)throw new IllegalArgumentException("Informe uma diária maior que zero.");
        if(form.getCapacidade()==null || form.getCapacidade()<1)throw new IllegalArgumentException("A capacidade mínima é de uma pessoa.");
        try(Connection c=connection()){
            String statusAtual=null;
            if(id!=null){
                try(PreparedStatement p=c.prepareStatement("SELECT status FROM quarto WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Quarto não encontrado.");statusAtual=r.getString(1);}}
            }
            if(id!=null && !form.getStatus().equals(statusAtual) && existeReservaAtiva(c,id))throw new IllegalStateException("Não altere a situação enquanto houver uma reserva ativa. Os outros dados do quarto podem ser editados.");
            String sql=id==null?"INSERT INTO quarto(numero,tipo,status,valor_diaria,capacidade,descricao) VALUES(?,?,?,?,?,?)":"UPDATE quarto SET numero=?,tipo=?,status=?,valor_diaria=?,capacidade=?,descricao=? WHERE id=?";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,form.getNumero().trim());p.setString(2,form.getTipo());p.setString(3,form.getStatus());p.setDouble(4,form.getValorDiaria());p.setInt(5,form.getCapacidade());p.setString(6,vazioNulo(form.getDescricao()));if(id!=null)p.setInt(7,id);p.executeUpdate();}
        }
    }

    public void excluir(int id) throws SQLException {
        try(Connection c=connection()){
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? LIMIT 1")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalStateException("Não exclua um quarto que possui histórico de reservas.");}}
            try(PreparedStatement p=c.prepareStatement("DELETE FROM quarto WHERE id=?")){p.setInt(1,id);if(p.executeUpdate()==0)throw new IllegalArgumentException("Quarto não encontrado.");}
        }
    }

    private boolean existeReservaAtiva(Connection c,Integer id)throws SQLException{
        if(id==null)return false;
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM reserva WHERE quarto_id=? AND status IN ('PENDENTE','CONFIRMADA','EM_HOSPEDAGEM') LIMIT 1")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){return r.next();}}
    }
    private String vazioNulo(String value){return value==null||value.isBlank()?null:value.trim();}

    public record Room(int id,String numero,String tipo,String status,double valorDiaria,int capacidade,String descricao,boolean semConflito){}
    public static class RoomForm {
        private String numero,tipo,status,descricao; private Double valorDiaria; private Integer capacidade;
        public String getNumero(){return numero;} public void setNumero(String v){numero=v;}
        public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public String getDescricao(){return descricao;} public void setDescricao(String v){descricao=v;}
        public Double getValorDiaria(){return valorDiaria;} public void setValorDiaria(Double v){valorDiaria=v;}
        public Integer getCapacidade(){return capacidade;} public void setCapacidade(Integer v){capacidade=v;}
    }
}
