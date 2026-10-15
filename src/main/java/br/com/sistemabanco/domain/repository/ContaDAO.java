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

    void movimentarComTransacao(ContaBancaria conta, Transacao transacao);

    void deletar(String cpf);
    List<ContaBancaria> buscarTodas();
    Optional<ContaBancaria> buscarPorCpf(String cpf);
}