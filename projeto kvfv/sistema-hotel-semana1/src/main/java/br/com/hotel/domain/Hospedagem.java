package br.com.hotel.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record Hospedagem(int id, Reserva reserva, LocalDateTime checkIn, LocalDateTime checkOut, List<Diaria> diarias) {
    public Hospedagem {
        if (reserva == null) throw new IllegalArgumentException("A hospedagem precisa estar ligada a uma reserva.");
        if (checkOut != null && checkIn == null) throw new IllegalArgumentException("Não pode haver check-out sem check-in.");
        if (checkOut != null && checkOut.isBefore(checkIn)) throw new IllegalArgumentException("Check-out não pode anteceder o check-in.");
        diarias = diarias == null ? List.of() : List.copyOf(diarias);
    }

    public BigDecimal calcularTotalDiarias() {
        return diarias.stream().map(Diaria::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
