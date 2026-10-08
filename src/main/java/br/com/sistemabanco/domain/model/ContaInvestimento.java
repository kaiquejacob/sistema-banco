package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;

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

    public BigDecimal calcularRendimentoMensal() {
        return getSaldo().multiply(TAXA_RENDIMENTO);
    }

    public BigDecimal getTaxaAdministracao() {
        return TAXA_ADMINISTRACAO;
    }
}