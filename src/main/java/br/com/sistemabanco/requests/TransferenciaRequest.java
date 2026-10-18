package br.com.sistemabanco.requests;

import br.com.sistemabanco.validation.Cpf;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequest(
        @NotBlank @Cpf String cpfOrigem,
        @NotBlank @Cpf String cpfDestino,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor
) {
}