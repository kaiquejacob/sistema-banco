package br.com.sistemabanco.presentation.mapper;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.ContaInvestimento;
import br.com.sistemabanco.domain.model.ContaPoupanca;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.presentation.responses.DetalhesResponse;
import br.com.sistemabanco.presentation.responses.EnderecoResponse;
import br.com.sistemabanco.presentation.responses.ExtratoResponse;
import br.com.sistemabanco.presentation.responses.TransacaoResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class ExtratoMapper {

    private ExtratoMapper() {
    }

    public static ExtratoResponse de(ContaBancaria conta) {
        List<TransacaoResponse> transacoes = conta.getHistorico().stream()
                .map(t -> new TransacaoResponse(
                        t.getTipo(), t.getTipo().getDescricao(), t.getValor(), t.getDataHora()))
                .toList();

        return new ExtratoResponse(
                conta.getCpf(),
                conta.getTitular(),
                conta.getTipoConta(),
                conta.getSaldo(),
                enderecoDe(conta.getEndereco()),
                transacoes,
                detalhesDe(conta)
        );
    }

    private static EnderecoResponse enderecoDe(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoResponse(
                endereco.cep(), endereco.logradouro(), endereco.bairro(),
                endereco.cidade(), endereco.estado());
    }

    private static DetalhesResponse detalhesDe(ContaBancaria conta) {
        return switch (conta) {
            case ContaCorrente corrente -> {
                int saques = corrente.getSaquesRealizados();
                int restantes = Math.max(0, corrente.getLimiteSaquesGratuitos() - saques);
                yield new DetalhesResponse(saques, restantes, null, null);
            }
            case ContaPoupanca poupanca ->
                    new DetalhesResponse(null, null, arredondar(poupanca.calcularRendimentoMensal()), null);
            case ContaInvestimento investimento ->
                    new DetalhesResponse(null, null,
                            arredondar(investimento.calcularRendimentoMensal()),
                            investimento.getTaxaAdministracao());
            default -> null;
        };
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}