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

class ReservationRepositoryTest {
    @TempDir Path temp;
    ReservationRepository repository;
    String url;

    @BeforeEach void setUp() throws Exception {
        url="jdbc:sqlite:"+temp.resolve("hotel-test.db");repository=new ReservationRepository();ReflectionTestUtils.setField(repository,"databaseUrl",url);
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE hospede(id INTEGER PRIMARY KEY,nome TEXT,email TEXT,ativo INTEGER NOT NULL DEFAULT 1)");
            s.executeUpdate("CREATE TABLE quarto(id INTEGER PRIMARY KEY,numero TEXT,tipo TEXT,status TEXT,valor_diaria REAL,capacidade INTEGER,descricao TEXT)");
            s.executeUpdate("CREATE TABLE reserva(id INTEGER PRIMARY KEY AUTOINCREMENT,hospede_id INTEGER,quarto_id INTEGER,data_entrada TEXT,data_saida TEXT,status TEXT)");
            s.executeUpdate("CREATE TABLE hospedagem(id INTEGER PRIMARY KEY AUTOINCREMENT,reserva_id INTEGER UNIQUE,checkin_em TEXT,checkout_em TEXT,valor_total REAL DEFAULT 0)");
            s.executeUpdate("INSERT INTO hospede(id,nome,email) VALUES(1,'Ana','ana@example.com'),(2,'Bia','bia@example.com')");
            s.executeUpdate("INSERT INTO quarto VALUES(10,'201','CASAL','DISPONIVEL',180,2,NULL),(20,'302','SUITE','DISPONIVEL',300,4,NULL)");
        }
    }

    @Test void rejectsOverlappingBookingsAndCancellationReleasesRoom() throws Exception {
        LocalDate start=LocalDate.now(),end=start.plusDays(2);
        assertTrue(repository.quartos(start,end).getFirst().disponivel());
        repository.criar(1,10,start,end);
        assertThrows(IllegalStateException.class,()->repository.criar(2,10,start.plusDays(1),end.plusDays(1)));
        int reservationId=repository.reservas(1).getFirst().id();
        repository.cancelar(reservationId,1);
        assertTrue(repository.quartos(start,end).getFirst().disponivel());
    }

    @Test void checkinAndCheckoutUpdateReservationRoomAndStay() throws Exception {
        LocalDate start=LocalDate.now(),end=start.plusDays(2);
        repository.criar(1,10,start,end);int id=repository.reservas(1).getFirst().id();repository.confirmar(id);repository.checkin(id);
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement();var r=s.executeQuery("SELECT r.status,q.status FROM reserva r JOIN quarto q ON q.id=r.quarto_id WHERE r.id="+id)){
            assertTrue(r.next());assertEquals("EM_HOSPEDAGEM",r.getString(1));assertEquals("OCUPADO",r.getString(2));
        }
        repository.checkout(id);
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement();var r=s.executeQuery("SELECT r.status,q.status,h.checkout_em,h.valor_total FROM reserva r JOIN quarto q ON q.id=r.quarto_id JOIN hospedagem h ON h.reserva_id=r.id WHERE r.id="+id)){
            assertTrue(r.next());assertEquals("CONCLUIDA",r.getString(1));assertEquals("DISPONIVEL",r.getString(2));assertNotNull(r.getString(3));assertEquals(180.0,r.getDouble(4));
        }
    }

    @Test void guestCannotModifyAnotherGuestsReservationAndMaintenanceBlocksBooking() throws Exception {
        LocalDate start=LocalDate.now(),end=start.plusDays(1);repository.criar(1,10,start,end);int id=repository.reservas(1).getFirst().id();
        assertThrows(IllegalArgumentException.class,()->repository.atualizar(id,2,2,20,start,end));
        try(Connection c=DriverManager.getConnection(url);Statement s=c.createStatement()){s.executeUpdate("UPDATE quarto SET status='MANUTENCAO' WHERE id=20");}
        assertThrows(IllegalStateException.class,()->repository.criar(2,20,start,end));
    }

    @Test void guestCannotBookTwoRoomsForOverlappingDatesButMayBookAnotherPeriod() throws Exception {
        LocalDate start=LocalDate.now(),end=start.plusDays(2);
        repository.criar(1,10,start,end);
        assertThrows(IllegalStateException.class,()->repository.criar(1,20,start.plusDays(1),end.plusDays(1)));
        assertDoesNotThrow(()->repository.criar(1,20,end,end.plusDays(2)));
    }

    @Test void deletesCancelledReservationsAndLeavesTheRoomAvailable() throws Exception {
        LocalDate start=LocalDate.now(),end=start.plusDays(1);
        repository.criar(1,10,start,end);
        int id=repository.reservas(1).getFirst().id();
        repository.cancelar(id,1);
        assertEquals(1,repository.excluirCanceladas());
        assertTrue(repository.reservas(1).isEmpty());
        assertTrue(repository.quartos(start,end).getFirst().disponivel());
    }
}
