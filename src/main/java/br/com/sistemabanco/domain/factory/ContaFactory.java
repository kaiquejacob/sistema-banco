package br.com.sistemabanco.domain.factory;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContaFactory {

    public static ContaBancaria criarConta(TipoConta tipo, String titular, BigDecimal saldo, String cpf, String email, Endereco endereco)
            throws CpfInvalidoException, EmailInvalidoException {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de conta não pode ser nulo.");
        }
        return switch (tipo) {
            case CORRENTE -> new ContaCorrente(titular, saldo, cpf, email, endereco);
            case POUPANCA -> new ContaPoupanca(titular, saldo, cpf, email, endereco);
            case INVESTIMENTO -> new ContaInvestimento(titular, saldo, cpf, email, endereco);
        };
    }

    public static ContaBancaria criarContaComData(TipoConta tipo, String titular, BigDecimal saldo, String cpf, String email, LocalDate dataAbertura, Endereco endereco)
            throws CpfInvalidoException, EmailInvalidoException {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de conta não pode ser nulo.");
        }
        return switch (tipo) {
            case CORRENTE -> new ContaCorrente(titular, saldo, cpf, email, dataAbertura, endereco);
            case POUPANCA -> new ContaPoupanca(titular, saldo, cpf, email, dataAbertura, endereco);
            case INVESTIMENTO -> new ContaInvestimento(titular, saldo, cpf, email, dataAbertura, endereco);
        };
    }

    public static ContaBancaria criarContaComEstado(TipoConta tipo, String titular, BigDecimal saldo, String cpf,
                                                     String email, LocalDate dataAbertura, Endereco endereco,
                                                     int saquesRealizados, LocalDate dataUltimoSaque)
            throws CpfInvalidoException, EmailInvalidoException {
        return switch (tipo) {
            case CORRENTE -> new ContaCorrente(titular, saldo, cpf, email, dataAbertura, endereco,
                    saquesRealizados, dataUltimoSaque);
            case POUPANCA -> new ContaPoupanca(titular, saldo, cpf, email, dataAbertura, endereco);
            case INVESTIMENTO -> new ContaInvestimento(titular, saldo, cpf, email, dataAbertura, endereco);
        };
    }
}