package br.com.hotel.domain;

import java.math.BigDecimal;

public record Quarto(int id, String numero, TipoQuarto tipo, StatusQuarto status, BigDecimal valorDiaria) {
    public Quarto {
        if (numero == null || numero.isBlank()) throw new IllegalArgumentException("Número do quarto é obrigatório.");
        if (tipo == null || status == null) throw new IllegalArgumentException("Tipo e situação do quarto são obrigatórios.");
        if (valorDiaria == null || valorDiaria.signum() <= 0) throw new IllegalArgumentException("A diária deve ter valor positivo.");
    }
}
