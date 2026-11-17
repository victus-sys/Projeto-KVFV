package br.com.hotel.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Diaria(LocalDate data, BigDecimal valor) {
    public Diaria {
        if (data == null) throw new IllegalArgumentException("Data da diária é obrigatória.");
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("O valor da diária deve ser positivo.");
    }
}
