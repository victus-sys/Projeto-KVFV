package br.com.hotel.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class HospedeRepositoryTest {
    @TempDir Path temp;
    private HospedeRepository repository;
    private String url;

    @BeforeEach void setUp() throws Exception {
        url="jdbc:sqlite:"+temp.resolve("guests.db");
        repository=new HospedeRepository();
        ReflectionTestUtils.setField(repository,"databaseUrl",url);
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE usuario(id INTEGER PRIMARY KEY,nome TEXT,login TEXT,senha TEXT,perfil TEXT)");
            s.executeUpdate("CREATE TABLE hospede(id INTEGER PRIMARY KEY,nome TEXT,documento TEXT,telefone TEXT,email TEXT,usuario_id INTEGER,ativo INTEGER NOT NULL DEFAULT 1)");
            s.executeUpdate("CREATE TABLE reserva(id INTEGER PRIMARY KEY,hospede_id INTEGER,quarto_id INTEGER,data_entrada TEXT,data_saida TEXT,status TEXT)");
            s.executeUpdate("CREATE TABLE hospedagem(id INTEGER PRIMARY KEY,reserva_id INTEGER,checkin_em TEXT,checkout_em TEXT,valor_total REAL)");
            s.executeUpdate("INSERT INTO usuario VALUES(10,'Ana','ana@example.com','hash','HOSPEDE')");
            s.executeUpdate("INSERT INTO hospede VALUES(1,'Ana',NULL,NULL,'ana@example.com',10,1),(2,'Bia',NULL,NULL,'bia@example.com',NULL,1)");
            s.executeUpdate("INSERT INTO reserva VALUES(100,1,5,'2026-12-01','2026-12-03','CONFIRMADA')");
            s.executeUpdate("INSERT INTO hospedagem VALUES(500,100,'2026-12-01T14:00:00','2026-12-03T12:00:00',360)");
        }
    }

    @Test void permanentlyDeletesGuestAndItsReservationsAndAccount() throws Exception {
        assertEquals(1,repository.excluir(1));
        assertTrue(repository.buscar("").stream().noneMatch(g -> g.id()==1));
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            try(var r=s.executeQuery("SELECT COUNT(*) FROM hospede WHERE id=1")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM reserva WHERE hospede_id=1")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM hospedagem WHERE reserva_id=100")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM usuario WHERE id=10")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
        }
    }

    @Test void permanentlyDeletesGuestWithoutReservationHistory() throws Exception {
        assertEquals(0,repository.excluir(2));
        assertNull(repository.buscarPorId(2));
    }

    @Test void protectsGuestWhoIsCurrentlyStayingAtTheHotel() throws Exception {
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("UPDATE reserva SET status='EM_HOSPEDAGEM' WHERE id=100");
        }
        assertThrows(IllegalStateException.class,()->repository.excluir(1));
        assertNotNull(repository.buscarPorId(1));
    }
}
