package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public class ContaCorrente extends ContaBancaria {
    private static final BigDecimal TAXA_SAQUE = new BigDecimal("10.00");
    private static final int LIMITE_SAQUES_GRATUITOS = 2;
    private int saquesRealizados;
    private LocalDate dataUltimoSaque;

    public ContaCorrente(String titular, BigDecimal saldo, String cpf, String email, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        this(titular, saldo, cpf, email, LocalDate.now(), endereco, 0, null);
    }

    public ContaCorrente(String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        this(titular, saldo, cpf, email, dataAbertura, endereco, 0, null);
    }

    public ContaCorrente(String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco, int saquesRealizados, LocalDate dataUltimoSaque) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.CORRENTE, cpf, email, dataAbertura, endereco);
        this.saquesRealizados = saquesRealizados;
        this.dataUltimoSaque = dataUltimoSaque;
    }

    private void verificarResetMensal() {
        if (dataUltimoSaque != null) {
            LocalDate hoje = LocalDate.now();
            if (hoje.getMonth() != dataUltimoSaque.getMonth() || hoje.getYear() != dataUltimoSaque.getYear()) {
                this.saquesRealizados = 0;
            }
        }
    }

    @Override
    public void sacar(BigDecimal valor) throws SaldoInsuficienteException {
        sacar(valor, TipoTransacao.SAQUE);
    }

    @Override
    public void sacar(BigDecimal valor, TipoTransacao tipo) throws SaldoInsuficienteException {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }

        verificarResetMensal();

        boolean cobraTaxa = saquesRealizados >= LIMITE_SAQUES_GRATUITOS;
        BigDecimal valorTotal = cobraTaxa ? valor.add(TAXA_SAQUE) : valor;

        try {
            super.sacar(valorTotal, tipo);
            saquesRealizados++;
            dataUltimoSaque = LocalDate.now();
        } catch (SaldoInsuficienteException e) {
            if (cobraTaxa) {
                throw new SaldoInsuficienteException(
                        "Valor do saque: R$" + valor +
                                " | Taxa de saque (franquia mensal excedida): R$" + TAXA_SAQUE +
                                " | Débito total necessário: R$" + valorTotal +
                                "\nSaldo atual em conta: R$" + getSaldo()
                );
            }
            throw e;
        }
    }

    @Override
    public String getDetalhesExtrato() {
        int saques = getSaquesRealizados();
        return String.format(Locale.ROOT,
                "🔢 Saques realizados este mês: %d (Gratuitos restantes: %d)\n",
                saques, Math.max(0, LIMITE_SAQUES_GRATUITOS - saques));
    }

    public int getSaquesRealizados() {
        verificarResetMensal();
        return saquesRealizados;
    }

    public LocalDate getDataUltimoSaque() {
        return dataUltimoSaque;
    }

    public int getLimiteSaquesGratuitos() {
        return LIMITE_SAQUES_GRATUITOS;
    }

    public BigDecimal getTaxaSaque() {
        return TAXA_SAQUE;
    }

    @Override
    public int getQuantidadeSaques() {
        return getSaquesRealizados();
    }
}