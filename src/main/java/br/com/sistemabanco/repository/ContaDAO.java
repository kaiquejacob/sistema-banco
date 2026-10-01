package br.com.sistemabanco.repository;

import br.com.sistemabanco.model.ContaBancaria;
import br.com.sistemabanco.model.Transacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContaDAO {
    void salvar(ContaBancaria conta);
    void atualizarSaldoESaques(ContaBancaria conta);
    void registrarTransacao(String cpf, Transacao transacao);
    void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor);
    void deletar(String cpf);
    List<ContaBancaria> buscarTodas();
    Optional<ContaBancaria> buscarPorCpf(String cpf);
}