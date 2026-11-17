package br.com.hotel.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RoomRepositoryTest {
    @TempDir Path temp;
    private RoomRepository rooms;
    private String url;

    @BeforeEach void setUp() throws Exception {
        url = "jdbc:sqlite:" + temp.resolve("rooms.db");
        rooms = new RoomRepository();
        ReflectionTestUtils.setField(rooms, "databaseUrl", url);
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE quarto(id INTEGER PRIMARY KEY,numero TEXT UNIQUE,tipo TEXT,status TEXT,valor_diaria REAL,capacidade INTEGER,descricao TEXT)");
            s.executeUpdate("CREATE TABLE reserva(id INTEGER PRIMARY KEY,quarto_id INTEGER,data_entrada TEXT,data_saida TEXT,status TEXT)");
            s.executeUpdate("INSERT INTO quarto VALUES(1,'101','CASAL','DISPONIVEL',150,2,NULL),(2,'102','SUITE','MANUTENCAO',250,3,NULL)");
        }
    }

    @Test void marksMaintenanceAndReservedRoomsAsUnavailableForSelectedDates() throws Exception {
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = start.plusDays(2);
        assertTrue(rooms.listarEm(start, end).get(0).semConflito());
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO reserva VALUES(1,1,'2026-12-01','2026-12-03','CONFIRMADA')");
        }
        var datedRooms = rooms.listarEm(start, end);
        assertFalse(datedRooms.get(0).semConflito());
        assertFalse(datedRooms.get(1).semConflito());
    }

    @Test void allowsBookingsForOccupiedRoomsOutsideTheStayAndReleasesCancelledRooms() throws Exception {
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("INSERT INTO quarto VALUES(3,'103','CASAL','OCUPADO',190,2,NULL)");
        }
        LocalDate start=LocalDate.of(2026,12,1),end=start.plusDays(2);
        assertTrue(rooms.listarEm(start,end).get(2).semConflito());
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("INSERT INTO reserva VALUES(1,3,'2026-12-01','2026-12-03','CONFIRMADA')");
        }
        assertFalse(rooms.listarEm(start,end).get(2).semConflito());
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("UPDATE reserva SET status='CANCELADA' WHERE id=1");
        }
        assertTrue(rooms.listarEm(start,end).get(2).semConflito());
    }

    @Test void createsRoomsAndEditsTheirInformationEvenWhenTheyHaveAnActiveBooking() throws Exception {
        RoomRepository.RoomForm form=new RoomRepository.RoomForm();
        form.setNumero("201");form.setTipo("CASAL");form.setStatus("DISPONIVEL");form.setValorDiaria(200.0);form.setCapacidade(2);form.setDescricao("Varanda");
        rooms.salvar(null,form);
        int id;
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement();var r=s.executeQuery("SELECT id FROM quarto WHERE numero='201'")){assertTrue(r.next());id=r.getInt(1);}
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("INSERT INTO reserva VALUES(1,"+id+",'2026-12-01','2026-12-03','CONFIRMADA')");
        }
        form.setValorDiaria(225.0);form.setDescricao("Varanda e vista");
        rooms.salvar(id,form);
        var saved=rooms.buscar(id);
        assertEquals(225.0,saved.valorDiaria());assertEquals("Varanda e vista",saved.descricao());
    }
}
