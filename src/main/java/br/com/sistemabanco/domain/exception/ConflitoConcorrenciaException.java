package br.com.sistemabanco.domain.exception;

public class ConflitoConcorrenciaException extends RuntimeException {
    public ConflitoConcorrenciaException(String message) {
        super(message);
    }

    public ConflitoConcorrenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
