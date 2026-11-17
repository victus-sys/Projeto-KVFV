
package br.com.hotel.domain;

import java.time.LocalDate;
import java.util.List;

public class Reserva {
    private final int id;
    private final Hospede hospede;
    private final Quarto quarto;
    private final LocalDate dataEntrada;
    private final LocalDate dataSaida;
    private StatusReserva status;

    public Reserva(int id, Hospede hospede, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida, StatusReserva status) {
        if (hospede == null || quarto == null) throw new IllegalArgumentException("Hóspede e quarto são obrigatórios.");
        if (dataEntrada == null || dataSaida == null || !dataSaida.isAfter(dataEntrada)) {
            throw new IllegalArgumentException("A saída deve ocorrer depois da entrada.");
        }
        if (status == null) throw new IllegalArgumentException("Situação da reserva é obrigatória.");
        this.id = id;
        this.hospede = hospede;
        this.quarto = quarto;
        this.dataEntrada = dataEntrada;
        this.dataSaida = dataSaida;
        this.status = status;
    }

    public void confirmar() { transicionar(StatusReserva.PENDENTE, StatusReserva.CONFIRMADA); }
    public void realizarCheckIn() { transicionar(StatusReserva.CONFIRMADA, StatusReserva.EM_HOSPEDAGEM); }
    public void realizarCheckOut() { transicionar(StatusReserva.EM_HOSPEDAGEM, StatusReserva.CONCLUIDA); }

    public void cancelar() {
        if (status != StatusReserva.PENDENTE && status != StatusReserva.CONFIRMADA) {
            throw new IllegalStateException("Só é possível cancelar uma reserva pendente ou confirmada.");
        }
        status = StatusReserva.CANCELADA;
    }

    public static boolean quartoDisponivel(Quarto quarto, LocalDate entrada, LocalDate saida, List<Reserva> reservas) {
        if (quarto == null || entrada == null || saida == null || !saida.isAfter(entrada)) {
            throw new IllegalArgumentException("Quarto e período válido são obrigatórios.");
        }

        if (quarto.status() == StatusQuarto.MANUTENCAO) return false;

        if (reservas == null) throw new IllegalArgumentException("A lista de reservas é obrigatória.");

        return reservas.stream()
                .filter(reserva -> reserva.quarto.id() == quarto.id())
                .filter(reserva -> reserva.status != StatusReserva.CANCELADA && reserva.status != StatusReserva.CONCLUIDA)
                .noneMatch(reserva -> entrada.isBefore(reserva.dataSaida) && reserva.dataEntrada.isBefore(saida));
    }

    private void transicionar(StatusReserva esperado, StatusReserva novo) {
        if (status != esperado) throw new IllegalStateException("Transição inválida da reserva: " + status + " para " + novo + ".");
        status = novo;
    }

    public int getId() { return id; }
    public Hospede getHospede() { return hospede; }
    public Quarto getQuarto() { return quarto; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public LocalDate getDataSaida() { return dataSaida; }
    public StatusReserva getStatus() { return status; }
}
