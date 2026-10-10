package br.com.sistemabanco.domain.model;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

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

    @Override
    public String getDetalhesExtrato() {
        return String.format(Locale.ROOT,
                "📈 Rendimento mensal estimado: R$%.2f\n📉 Taxa de administração: R$%s\n",
                calcularRendimentoMensal(), TAXA_ADMINISTRACAO);
    }
}