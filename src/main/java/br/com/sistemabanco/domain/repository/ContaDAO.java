package br.com.sistemabanco.domain.repository;

import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ResumoConta;
import br.com.sistemabanco.domain.model.Transacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContaDAO {
    void salvar(ContaBancaria conta);

    void movimentarComTransacao(ContaBancaria conta, List<Transacao> novasTransacoes);

    void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor);

    void deletar(String cpf);

    List<ResumoConta> listarResumosPorSaldo(int pagina, int tamanho);

    long contarContas();

    Optional<ContaBancaria> buscarPorCpf(String cpf);
}