package br.com.sistemabanco.model;

import br.com.sistemabanco.exception.CpfInvalidoException;
import br.com.sistemabanco.exception.EmailInvalidoException;
import br.com.sistemabanco.exception.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public void calcularExtrato() {
        verificarResetMensal();
        super.calcularExtrato();
        System.out.println("🔢 Saques realizados este mês: " + saquesRealizados +
                " (Gratuitos restantes: " + Math.max(0, LIMITE_SAQUES_GRATUITOS - saquesRealizados) + ")");
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

    public int getSaquesRealizados() {
        verificarResetMensal();
        return saquesRealizados;
    }

    public LocalDate getDataUltimoSaque() {
        return dataUltimoSaque;
    }

    @Override
    public int getQuantidadeSaques() {
        return getSaquesRealizados();
    }
}