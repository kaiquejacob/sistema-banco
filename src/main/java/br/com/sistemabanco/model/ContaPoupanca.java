package br.com.sistemabanco.model;

import br.com.sistemabanco.exception.CpfInvalidoException;
import br.com.sistemabanco.exception.EmailInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContaPoupanca extends ContaBancaria {

    private static final BigDecimal TAXA_RENDIMENTO = new BigDecimal("0.005");

    public ContaPoupanca(String titular, BigDecimal saldo, String cpf, String email, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.POUPANCA, cpf, email, LocalDate.now(), endereco);
    }

    public ContaPoupanca(String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco) throws CpfInvalidoException, EmailInvalidoException {
        super(titular, saldo, TipoConta.POUPANCA, cpf, email, dataAbertura, endereco);
    }

    @Override
    public void calcularExtrato() {
        super.calcularExtrato();
        BigDecimal rendimento = getSaldo().multiply(TAXA_RENDIMENTO);
        System.out.println("📈 Rendimento mensal estimado: R$" + String.format("%.2f", rendimento));
    }
}