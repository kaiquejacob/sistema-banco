package br.com.sistemabanco.presentation.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import br.com.sistemabanco.presentation.validation.Cpf;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequest(
        @Schema(description = "CPF da conta de destino", example = "11144477735")
        @NotBlank @Cpf
        String cpfDestino,

        @Schema(description = "Valor a transferir", example = "100.00")
        @NotNull @Positive @Digits(integer = 13, fraction = 2)
        BigDecimal valor
) {
}