package br.com.sistemabanco.domain.exception;

public class ServicoCepException extends RuntimeException {
    public ServicoCepException(String message) {
        super(message);
    }

    public ServicoCepException(String message, Throwable cause) {
        super(message, cause);
    }
}