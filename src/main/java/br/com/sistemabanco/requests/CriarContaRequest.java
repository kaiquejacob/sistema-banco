package br.com.sistemabanco.requests;

import br.com.sistemabanco.domain.model.TipoConta;
import br.com.sistemabanco.validation.Cpf;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarContaRequest(
        @NotBlank @Size(max = 100) String titular,
        @NotBlank @Cpf String cpf,
        @NotBlank @Size(max = 100) @Email String email,
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP deve ter 8 dígitos") String cep,
        @NotNull TipoConta tipo,
        @NotNull @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal saldoInicial,
        @NotBlank @Size(min = 8, max = 64) String senha
) {

    @Override
    public String toString() {
        return "CriarContaRequest[titular=" + titular + ", cpf=" + cpf + ", email=" + email
                + ", cep=" + cep + ", tipo=" + tipo + ", saldoInicial=" + saldoInicial + ", senha=***]";
    }
}