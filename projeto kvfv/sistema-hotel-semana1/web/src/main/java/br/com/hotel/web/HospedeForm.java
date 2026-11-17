package br.com.hotel.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class HospedeForm {
    @NotBlank(message = "Informe o nome do hóspede.")
    private String nome;
    private String documento;
    private String telefone;
    @Email(message = "Confira o formato do e-mail.")
    private String email;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
