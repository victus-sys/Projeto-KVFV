package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class ReportRepository {
    @Value("${hotel.database.url}") private String databaseUrl;
    public Map<String,Integer> resumo()throws SQLException{
        Map<String,Integer> result=new LinkedHashMap<>();
        try(Connection c=DriverManager.getConnection(databaseUrl);Statement s=c.createStatement()){
            result.put("hospedes",contar(s,"SELECT COUNT(*) FROM hospede WHERE ativo=1"));
            result.put("quartos",contar(s,"SELECT COUNT(*) FROM quarto"));
            result.put("disponiveis",contar(s,"SELECT COUNT(*) FROM quarto WHERE status='DISPONIVEL' AND valor_diaria>0"));
            result.put("ocupados",contar(s,"SELECT COUNT(*) FROM quarto WHERE status='OCUPADO'"));
            result.put("manutencao",contar(s,"SELECT COUNT(*) FROM quarto WHERE status='MANUTENCAO'"));
            result.put("reservas",contar(s,"SELECT COUNT(*) FROM reserva"));
            result.put("pendentes",contar(s,"SELECT COUNT(*) FROM reserva WHERE status='PENDENTE'"));
            result.put("confirmadas",contar(s,"SELECT COUNT(*) FROM reserva WHERE status='CONFIRMADA'"));
            result.put("hospedagensAtivas",contar(s,"SELECT COUNT(*) FROM reserva WHERE status='EM_HOSPEDAGEM'"));
        }
        return result;
    }
    private int contar(Statement s,String sql)throws SQLException{try(ResultSet r=s.executeQuery(sql)){return r.next()?r.getInt(1):0;}}
}
