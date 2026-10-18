package br.com.sistemabanco.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ValorRequest(
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor
) {
}