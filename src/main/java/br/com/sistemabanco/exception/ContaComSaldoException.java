package br.com.sistemabanco.exception;

public class ContaComSaldoException extends RuntimeException {
    public ContaComSaldoException(String message) {
        super(message);
    }
}