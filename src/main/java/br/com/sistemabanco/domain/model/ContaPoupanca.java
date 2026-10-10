package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public class ContaPoupanca extends ContaBancaria {

    private static final BigDecimal TAXA_RENDIMENTO = new BigDecimal("0.005");

    public ContaPoupanca(String titular, BigDecimal saldo, String cpf, String email, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.POUPANCA, cpf, email, LocalDate.now(), endereco);
    }

    public ContaPoupanca(String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.POUPANCA, cpf, email, dataAbertura, endereco);
    }

    public BigDecimal calcularRendimentoMensal() {
        return getSaldo().multiply(TAXA_RENDIMENTO);
    }

    @Override
    public String getDetalhesExtrato() {
        return String.format(Locale.ROOT,
                "📈 Rendimento mensal estimado: R$%.2f\n",
                calcularRendimentoMensal());
    }
}