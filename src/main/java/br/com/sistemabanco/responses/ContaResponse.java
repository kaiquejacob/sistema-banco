package br.com.sistemabanco.responses;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ResumoConta;
import br.com.sistemabanco.domain.model.TipoConta;

import java.math.BigDecimal;

public record ContaResponse(String cpf, String titular, TipoConta tipo, BigDecimal saldo) {

    public static ContaResponse de(ContaBancaria conta) {
        return new ContaResponse(
                conta.getCpf(), conta.getTitular(), conta.getTipoConta(), conta.getSaldo());
    }

    public static ContaResponse de(ResumoConta resumo) {
        return new ContaResponse(
                resumo.cpf(), resumo.titular(), resumo.tipo(), resumo.saldo());
    }
}