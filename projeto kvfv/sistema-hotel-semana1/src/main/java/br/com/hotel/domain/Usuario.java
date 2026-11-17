package br.com.hotel.domain;

public record Usuario(int id, String nome, String login, String senhaHash, PerfilAcesso perfil) {
    public Usuario {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do usuário é obrigatório.");
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login é obrigatório.");
        if (senhaHash == null || senhaHash.isBlank()) throw new IllegalArgumentException("Senha é obrigatória.");
        if (perfil == null) throw new IllegalArgumentException("Perfil de acesso é obrigatório.");
    }
}
