package br.com.hotel.domain;

public record Funcionario(int id, String nome, String documento, String cargo, Usuario usuario) {
    public Funcionario {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do funcionário é obrigatório.");
        if (cargo == null || cargo.isBlank()) throw new IllegalArgumentException("Cargo é obrigatório.");
        if (usuario == null || usuario.perfil() == PerfilAcesso.HOSPEDE) {
            throw new IllegalArgumentException("Funcionário precisa de uma conta de acesso interna.");
        }
    }
}
