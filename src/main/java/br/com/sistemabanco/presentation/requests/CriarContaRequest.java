package br.com.sistemabanco.presentation.requests;

import br.com.sistemabanco.domain.model.TipoConta;
import io.swagger.v3.oas.annotations.media.Schema;
import br.com.sistemabanco.presentation.validation.Cpf;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarContaRequest(
        @Schema(description = "Nome completo do titular", example = "Kaíque Jacob")
        @NotBlank @Size(max = 100)
        String titular,

        @Schema(description = "CPF do titular, apenas dígitos", example = "52998224725")
        @NotBlank @Cpf
        String cpf,

        @Schema(description = "E-mail do titular", example = "kaique@example.com")
        @NotBlank @Size(max = 100) @Email
        String email,

        @Schema(description = "CEP para consulta de endereço", example = "01001000")
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 dígitos")
        String cep,

        @Schema(description = "Tipo da conta", example = "CORRENTE")
        @NotNull
        TipoConta tipo,

        @Schema(description = "Saldo inicial da conta", example = "100.00")
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2)
        BigDecimal saldoInicial,

        @Schema(description = "Senha de acesso, com no mínimo 8 caracteres", example = "SenhaForte#1")
        @NotBlank @Size(min = 8, max = 64)
        String senha
) {

    @Override
    public String toString() {
        return "CriarContaRequest[titular=" + titular + ", cpf=" + cpf + ", email=" + email
                + ", cep=" + cep + ", tipo=" + tipo + ", saldoInicial=" + saldoInicial + ", senha=***]";
    }
}