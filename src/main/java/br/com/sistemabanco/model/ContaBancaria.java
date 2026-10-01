package br.com.sistemabanco.model;

import br.com.sistemabanco.util.Validador;
import br.com.sistemabanco.exception.CpfInvalidoException;
import br.com.sistemabanco.exception.EmailInvalidoException;
import br.com.sistemabanco.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ContaBancaria {
    private String titular;
    protected BigDecimal saldo;
    private TipoConta tipoConta;
    private String cpf;
    private String email;
    private LocalDate dataAbertura;
    protected List<Transacao> historico = new ArrayList<>();
    private Endereco endereco;

    public ContaBancaria(String cpf, String titular, BigDecimal saldo, TipoConta tipoConta, String email, Endereco endereco) {
        this.cpf = cpf;
        this.titular = titular;
        this.saldo = saldo;
        this.tipoConta = tipoConta;
        this.email = email;
        this.dataAbertura = LocalDate.now();
        this.endereco = endereco;
    }

    public ContaBancaria(String titular, BigDecimal saldo, TipoConta tipoConta, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        if (!Validador.validarCpf(cpf)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        if (!Validador.validarEmail(email)) {
            throw new EmailInvalidoException("E-mail inválido: " + email);
        }

        this.titular = titular;
        this.saldo = (saldo != null) ? saldo : BigDecimal.ZERO;
        this.tipoConta = tipoConta;
        this.cpf = Validador.normalizarCpf(cpf);
        this.email = email;
        this.dataAbertura = (dataAbertura != null) ? dataAbertura : LocalDate.now();
        this.endereco = endereco;

        if (this.saldo.compareTo(BigDecimal.ZERO) > 0 && dataAbertura == null) {
            this.historico.add(new Transacao(TipoTransacao.ABERTURA_CONTA, this.saldo));
        }
    }


    public void depositar(BigDecimal valor) {
        depositar(valor, TipoTransacao.DEPOSITO);
    }


    public void depositar(BigDecimal valor, TipoTransacao tipo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        this.saldo = this.saldo.add(valor);
        this.historico.add(new Transacao(tipo, valor));
    }


    public void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        sacar(valor, TipoTransacao.SAQUE);
    }


    public void sacar(BigDecimal valor, TipoTransacao tipo) throws SaldoInsuficienteException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Saldo atual: R$" + saldo + ", valor solicitado: R$" + valor);
        }
        this.saldo = this.saldo.subtract(valor);
        this.historico.add(new Transacao(tipo, valor));
    }

    public void calcularExtrato() {
        System.out.println("\n👤 Titular: " + getTitular());
        System.out.println("💼 Tipo: " + getTipoConta().nome);
        System.out.println("💵 Saldo atual: R$" + getSaldo());
        if (endereco != null) {
            System.out.println("📍 Endereço: " + endereco);
        }
        System.out.println("\n--- 📜 Histórico de Movimentações ---");
        if (historico.isEmpty()) {
            System.out.println("Nenhuma transação registrada.");
        } else {
            for (Transacao t : historico) {
                System.out.println(t);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Titular: %-15s | CPF: %-11s\nEndereço: %s\nTipo: %-12s | Saldo: R$ %10.2f\n",
                titular, cpf, (endereco != null ? endereco : "Não cadastrado"), tipoConta.nome, saldo);
    }

    public String getTitular() {
        return titular;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public TipoConta getTipoConta() {
        return tipoConta;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getDataAbertura() {
        return dataAbertura;
    }

    public List<Transacao> getHistorico() {
        return historico;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public void adicionarTransacaoExistente(Transacao transacao) {
        this.historico.add(transacao);
    }

    public int getQuantidadeSaques() {
        return 0;
    }
}