package br.com.hotel.domain;

public record Endereco(String logradouro, String numero, String cidade, String estado, String cep) {
    public Endereco {
        if (logradouro == null || logradouro.isBlank() || cidade == null || cidade.isBlank()) {
            throw new IllegalArgumentException("Logradouro e cidade são obrigatórios.");
        }
    }
}
