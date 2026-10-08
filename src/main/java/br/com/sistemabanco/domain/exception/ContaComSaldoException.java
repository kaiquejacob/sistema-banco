package br.com.sistemabanco.domain.exception;

public class ContaComSaldoException extends RuntimeException {
    public ContaComSaldoException(String message) {
        super(message);
    }
}