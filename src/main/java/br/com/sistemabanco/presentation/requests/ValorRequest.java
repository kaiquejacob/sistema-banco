package br.com.sistemabanco.presentation.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ValorRequest(
        @Schema(description = "Valor da operação", example = "250.00")
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal valor
) {
}