package br.com.sistemabanco.presentation.responses;

import br.com.sistemabanco.domain.model.PaginaDeContas;

import java.util.List;

public record PaginaResponse<T>(List<T> itens, long total, int pagina, int tamanho, int totalDePaginas) {

    public static PaginaResponse<ContaResponse> de(PaginaDeContas pagina) {
        List<ContaResponse> itens = pagina.itens().stream()
                .map(ContaResponse::de)
                .toList();
        return new PaginaResponse<>(
                itens, pagina.total(), pagina.pagina(), pagina.tamanho(), pagina.totalDePaginas());
    }
}