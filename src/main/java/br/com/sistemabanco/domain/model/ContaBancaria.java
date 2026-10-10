package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.util.Validador;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    private BigDecimal valorSaquesHoje = BigDecimal.ZERO;
    private LocalDate dataControleSaques = LocalDate.now();

    public ContaBancaria(String cpf, String titular, BigDecimal saldo, TipoConta tipoConta, String email, Endereco endereco) {
        this(titular, saldo, tipoConta, cpf, email, LocalDate.now(), endereco);
    }

    public ContaBancaria(String titular, BigDecimal saldo, TipoConta tipoConta, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        if (!Validador.validarCpf(cpf)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        if (!Validador.validarEmail(email)) {
            throw new EmailInvalidoException("E-mail inválido: " + email);
        }
        if (!Validador.validarNome(titular)) {
            throw new IllegalArgumentException("Nome do titular inválido.");
        }
        if (tipoConta == null) {
            throw new IllegalArgumentException("Tipo de conta não pode ser nulo.");
        }
        if (saldo != null && saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O saldo não pode ser negativo.");
        }

        this.titular = titular;
        this.saldo = (saldo != null) ? saldo : BigDecimal.ZERO;
        this.tipoConta = tipoConta;
        this.cpf = Validador.normalizarCpf(cpf);
        this.email = email;
        this.dataAbertura = (dataAbertura != null) ? dataAbertura : LocalDate.now();
        this.endereco = endereco;

    }


    public void depositar(BigDecimal valor) {
        depositar(valor, TipoTransacao.DEPOSITO);
    }


    public void depositar(BigDecimal valor, TipoTransacao tipo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de transação não pode ser nulo.");
        }
        this.saldo = this.saldo.add(valor);
        this.historico.add(new Transacao(tipo, valor));
    }


    public void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        sacar(valor, TipoTransacao.SAQUE);
    }


    public void sacar(BigDecimal valor, TipoTransacao tipo) throws SaldoInsuficienteException {
        validarValor(valor, "saque");
        debitar(valor, tipo);
    }


    public void debitarTransferencia(BigDecimal valor) {
        validarValor(valor, "transferência");
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Saldo atual: R$" + saldo + ", valor solicitado: R$" + valor);
        }
        saldo = saldo.subtract(valor);
        historico.add(new Transacao(TipoTransacao.TRANSFERENCIA_ENVIADA, valor));
    }

    public void debitarTarifa(BigDecimal valor, TipoTransacao tipo) {
        validarValor(valor, "tarifa");
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para cobrança da tarifa.");
        }
        saldo = saldo.subtract(valor);
        historico.add(new Transacao(tipo, valor));
    }

    protected void debitar(BigDecimal valor, TipoTransacao tipo) throws SaldoInsuficienteException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser positivo.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de transação não pode ser nulo.");
        }
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente. Saldo atual: R$" + saldo + ", valor solicitado: R$" + valor);
        }
        registrarSaque(valor);
        this.saldo = this.saldo.subtract(valor);
        this.historico.add(new Transacao(tipo, valor));
    }

    private void registrarSaque(BigDecimal valor) {
        if (!LocalDate.now().equals(dataControleSaques)) {
            valorSaquesHoje = BigDecimal.ZERO;
            dataControleSaques = LocalDate.now();
        }
        BigDecimal limite = tipoConta.obterLimiteSaqueDiario();
        if (valorSaquesHoje.add(valor).compareTo(limite) > 0) {
            throw new SaldoInsuficienteException("Limite diário de saque excedido. Limite: R$" + limite);
        }
        valorSaquesHoje = valorSaquesHoje.add(valor);
    }

    private void validarValor(BigDecimal valor, String operacao) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do " + operacao + " deve ser positivo.");
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
        return Collections.unmodifiableList(historico);
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public void adicionarTransacaoExistente(Transacao transacao) {
        if (transacao == null) {
            throw new IllegalArgumentException("Transação não pode ser nula.");
        }
        this.historico.add(transacao);
    }


    public void limparHistorico() {
        this.historico.clear();
    }

    public int getQuantidadeSaques() {
        return 0;
    }

    public BigDecimal getValorSaquesHoje() {
        if (!LocalDate.now().equals(dataControleSaques)) {
            valorSaquesHoje = BigDecimal.ZERO;
            dataControleSaques = LocalDate.now();
        }
        return valorSaquesHoje;
    }
}