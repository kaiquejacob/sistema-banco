package br.com.sistemabanco.domain.model;

import java.util.List;

public record PaginaDeContas(List<ResumoConta> itens, long total, int pagina, int tamanho) {

    public int totalDePaginas() {
        return (int) ((total + tamanho - 1) / tamanho);
    }
}