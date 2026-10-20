package br.com.sistemabanco.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @Column(name = "login", nullable = false, length = 30)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private Papel papel;

    @Column(name = "cpf_titular", length = 11)
    private String cpfTitular;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(String login, String senhaHash, Papel papel, String cpfTitular) {
        this.login = login;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.cpfTitular = cpfTitular;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public String getCpfTitular() {
        return cpfTitular;
    }
}