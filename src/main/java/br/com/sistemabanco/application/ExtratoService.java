package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.Transacao;

/**
 * Formata o extrato para a interface de console sem acoplar as entidades a
 * System.out.
 */
public class ExtratoService {

    public String gerar(ContaBancaria conta) {
        if (conta == null) {
            throw new IllegalArgumentException("Conta não pode ser nula.");
        }

        StringBuilder extrato = new StringBuilder();
        extrato.append("\n👤 Titular: ").append(conta.getTitular()).append('\n')
                .append("💼 Tipo: ").append(conta.getTipoConta().getNome()).append('\n')
                .append("💵 Saldo atual: R$").append(conta.getSaldo()).append('\n');
        if (conta.getEndereco() != null) {
            extrato.append("📍 Endereço: ").append(conta.getEndereco()).append('\n');
        }
        extrato.append("\n--- 📜 Histórico de Movimentações ---\n");
        if (conta.getHistorico().isEmpty()) {
            extrato.append("Nenhuma transação registrada.\n");
        } else {
            for (Transacao transacao : conta.getHistorico()) {
                extrato.append(transacao).append('\n');
            }
        }

        String detalhes = conta.getDetalhesExtrato();
        if (!detalhes.isEmpty()) {
            extrato.append(detalhes);
        }

        return extrato.toString();
    }
}
