package br.com.hotel.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Pagamento(int id, Hospedagem hospedagem, BigDecimal valor, FormaPagamento forma, LocalDateTime data) {
    public Pagamento {
        if (hospedagem == null) throw new IllegalArgumentException("Pagamento precisa estar ligado a uma hospedagem.");
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("O valor do pagamento deve ser positivo.");
        if (forma == null || data == null) throw new IllegalArgumentException("Forma e data do pagamento são obrigatórias.");
    }
}
