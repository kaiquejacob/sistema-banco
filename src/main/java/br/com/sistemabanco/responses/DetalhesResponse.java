package br.com.sistemabanco.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetalhesResponse(
        Integer saquesRealizadosNoMes,
        Integer saquesGratuitosRestantes,
        BigDecimal rendimentoMensalEstimado,
        BigDecimal taxaAdministracao
) {
}