package br.com.sistemabanco.infrastructure.persistence.entity;

import br.com.sistemabanco.domain.model.TipoConta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contas")
public class ContaEntity {

    @Id
    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "titular", nullable = false, length = 100)
    private String titular;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoConta tipo;

    @Column(name = "data_abertura", nullable = false)
    private LocalDate dataAbertura;

    @Column(name = "saques_realizados", nullable = false)
    private int saquesRealizados;

    @Column(name = "data_ultimo_saque")
    private LocalDate dataUltimoSaque;

    @OneToOne(mappedBy = "conta")
    private EnderecoEntity endereco;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ContaEntity() {

    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDate dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public int getSaquesRealizados() {
        return saquesRealizados;
    }

    public void setSaquesRealizados(int saquesRealizados) {
        this.saquesRealizados = saquesRealizados;
    }

    public LocalDate getDataUltimoSaque() {
        return dataUltimoSaque;
    }

    public void setDataUltimoSaque(LocalDate dataUltimoSaque) {
        this.dataUltimoSaque = dataUltimoSaque;
    }

    public EnderecoEntity getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoEntity endereco) {
        this.endereco = endereco;
    }

    public Long getVersion() {
        return version;
    }


}
