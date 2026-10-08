package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.ContaInvestimento;
import br.com.sistemabanco.domain.model.ContaPoupanca;
import br.com.sistemabanco.domain.model.Transacao;

import java.util.Locale;

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
                .append("💼 Tipo: ").append(conta.getTipoConta().nome).append('\n')
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

        if (conta instanceof ContaCorrente corrente) {
            int saques = corrente.getSaquesRealizados();
            extrato.append("🔢 Saques realizados este mês: ").append(saques)
                    .append(" (Gratuitos restantes: ")
                    .append(Math.max(0, corrente.getLimiteSaquesGratuitos() - saques))
                    .append(")\n");
        } else if (conta instanceof ContaPoupanca poupanca) {
            extrato.append("📈 Rendimento mensal estimado: R$")
                    .append(String.format(Locale.ROOT, "%.2f", poupanca.calcularRendimentoMensal()))
                    .append('\n');
        } else if (conta instanceof ContaInvestimento investimento) {
            extrato.append("📈 Rendimento mensal estimado: R$")
                    .append(String.format(Locale.ROOT, "%.2f", investimento.calcularRendimentoMensal()))
                    .append('\n')
                    .append("📉 Taxa de administração: R$")
                    .append(investimento.getTaxaAdministracao()).append('\n');
        }
        return extrato.toString();
    }
}
