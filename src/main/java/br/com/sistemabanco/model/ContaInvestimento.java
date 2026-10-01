package br.com.sistemabanco.model;

import br.com.sistemabanco.exception.CpfInvalidoException;
import br.com.sistemabanco.exception.EmailInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContaInvestimento extends ContaBancaria {
    private static final BigDecimal TAXA_RENDIMENTO = new BigDecimal("0.012");
    private static final BigDecimal TAXA_ADMINISTRACAO = new BigDecimal("20.00");

    public ContaInvestimento(String titular, BigDecimal saldo, String cpf, String email, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.INVESTIMENTO, cpf, email, LocalDate.now(), endereco);
    }

    public ContaInvestimento(String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.INVESTIMENTO, cpf, email, dataAbertura, endereco);
    }

    @Override
    public void calcularExtrato() {
        super.calcularExtrato();
        BigDecimal rendimento = getSaldo().multiply(TAXA_RENDIMENTO);
        System.out.println("📈 Rendimento mensal estimado: R$" + String.format("%.2f", rendimento));
        System.out.println("📉 Taxa de administração: R$" + TAXA_ADMINISTRACAO);
    }
}