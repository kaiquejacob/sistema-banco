package br.com.sistemabanco.presentation.responses;

import br.com.sistemabanco.domain.model.TipoConta;

import java.math.BigDecimal;
import java.util.List;

public record ExtratoResponse(
        String cpf,
        String titular,
        TipoConta tipo,
        BigDecimal saldo,
        EnderecoResponse endereco,
        List<TransacaoResponse> transacoes,
        DetalhesResponse detalhes
) {
}