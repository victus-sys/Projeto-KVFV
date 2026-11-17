package br.com.hotel.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReservaTest {
    private Hospede guest(){return new Hospede(1,"Ana",null,null,"ana@example.com");}
    private Quarto room(){return new Quarto(7,"202",TipoQuarto.CASAL,StatusQuarto.DISPONIVEL,new BigDecimal("180.00"));}

    @Test void reservaValidaAsTransicoesDeHospedagem(){
        Reserva reserva=new Reserva(1,guest(),room(),LocalDate.now(),LocalDate.now().plusDays(2),StatusReserva.PENDENTE);
        reserva.confirmar();assertEquals(StatusReserva.CONFIRMADA,reserva.getStatus());
        reserva.realizarCheckIn();assertEquals(StatusReserva.EM_HOSPEDAGEM,reserva.getStatus());
        reserva.realizarCheckOut();assertEquals(StatusReserva.CONCLUIDA,reserva.getStatus());
        assertThrows(IllegalStateException.class,reserva::cancelar);
    }

    @Test void naoPermiteDatasInvalidas(){
        assertThrows(IllegalArgumentException.class,()->new Reserva(1,guest(),room(),LocalDate.now(),LocalDate.now(),StatusReserva.PENDENTE));
    }

    @Test void reservaCanceladaLiberaQuartoENaoPermiteConflito(){
        LocalDate start=LocalDate.now();Reserva ativa=new Reserva(1,guest(),room(),start,start.plusDays(2),StatusReserva.CONFIRMADA);
        Reserva sobreposta=new Reserva(2,guest(),room(),start.plusDays(1),start.plusDays(3),StatusReserva.PENDENTE);
        assertFalse(Reserva.quartoDisponivel(room(),sobreposta.getDataEntrada(),sobreposta.getDataSaida(),List.of(ativa)));
        ativa.cancelar();
        assertTrue(Reserva.quartoDisponivel(room(),sobreposta.getDataEntrada(),sobreposta.getDataSaida(),List.of(ativa)));
    }

    @Test void quartoEmManutencaoNaoFicaDisponivel(){
        Quarto manutencao=new Quarto(9,"301",TipoQuarto.SUITE,StatusQuarto.MANUTENCAO,new BigDecimal("300"));
        assertFalse(Reserva.quartoDisponivel(manutencao,LocalDate.now(),LocalDate.now().plusDays(1),List.of()));
    }
}
