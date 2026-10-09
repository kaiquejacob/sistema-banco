package br.com.sistemabanco.domain.repository;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.Transacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContaDAO {
    void salvar(ContaBancaria conta);
    void atualizarSaldoESaques(ContaBancaria conta);
    void registrarTransacao(String cpf, Transacao transacao);
    void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor);

    /**
     * Implementações JDBC sobrescrevem este método para executar saldo e
     * histórico na mesma transação. O comportamento padrão mantém
     * compatibilidade com DAOs de testes e implementações legadas.
     */
    default void movimentarComTransacao(ContaBancaria conta, Transacao transacao) {
        atualizarSaldoESaques(conta);
        registrarTransacao(conta.getCpf(), transacao);
    }

    default void movimentarComTransacao(ContaBancaria conta, Transacao transacao, BigDecimal saldoAnterior) {
        movimentarComTransacao(conta, transacao);
    }

    default void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor,
                                        BigDecimal saldoOrigemAnterior, BigDecimal saldoDestinoAnterior) {
        transferirComTransacao(origem, destino, valor);
    }

    void deletar(String cpf);
    List<ContaBancaria> buscarTodas();
    Optional<ContaBancaria> buscarPorCpf(String cpf);
}