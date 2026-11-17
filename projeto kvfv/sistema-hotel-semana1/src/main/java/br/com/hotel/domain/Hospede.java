package br.com.hotel.domain;

public class Hospede {
    private final int id;
    private final String nome;
    private final String documento;
    private final String telefone;
    private final String email;
    private final Usuario usuario;
    private final Endereco endereco;

    public Hospede(int id, String nome, String documento, String telefone, String email) {
        this(id, nome, documento, telefone, email, null, null);
    }

    public Hospede(int id, String nome, String documento, String telefone, String email, Usuario usuario) {
        this(id, nome, documento, telefone, email, usuario, null);
    }

    public Hospede(int id, String nome, String documento, String telefone, String email, Usuario usuario, Endereco endereco) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome do hóspede é obrigatório.");
        if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.documento = documento;
        this.telefone = telefone;
        this.email = email;
        this.usuario = usuario;
        this.endereco = endereco;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public Usuario getUsuario() { return usuario; }
    public Endereco getEndereco() { return endereco; }
}
