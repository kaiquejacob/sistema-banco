package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.exception.ContaComSaldoException;
import br.com.sistemabanco.domain.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.factory.ContaFactory;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.service.CepService;
import br.com.sistemabanco.util.Validador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContaService {

    private final ContaDAO contaDAO;
    private final CepService cepService;

    public ContaService(ContaDAO contaDAO, CepService cepService) {
        if (contaDAO == null || cepService == null) {
            throw new IllegalArgumentException("As dependências do serviço não podem ser nulas.");
        }
        this.contaDAO = contaDAO;
        this.cepService = cepService;
    }

    public void criarConta(TipoConta tipo, String titular, BigDecimal saldo, String cpf, String email, String cep) {
        String cpfNormalizado = validarCpf(cpf);
        if (contaDAO.buscarPorCpf(cpfNormalizado).isPresent()) {
            throw new CpfJaCadastradoException("Já existe uma conta cadastrada para o CPF: " + cpfNormalizado);
        }
        Endereco endereco = cepService.buscarEnderecoPorCep(cep);
        ContaBancaria conta = ContaFactory.criarConta(tipo, titular, saldo, cpfNormalizado, email, endereco);
        contaDAO.salvar(conta);
    }

    @Transactional
    public void depositar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        conta.depositar(valor);
        Transacao transacao = new Transacao(TipoTransacao.DEPOSITO, valor);
        persistirMovimentacao(conta, transacao);

    }

    @Transactional
    public void sacar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        conta.sacar(valor);
        Transacao transacao = new Transacao(TipoTransacao.SAQUE, valor);
        persistirMovimentacao(conta, transacao);
    }

    @Transactional
    public void cobrarTaxaManutencao(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        BigDecimal taxa = conta.getTipoConta().calcularTaxaManutencao();
        if (taxa.signum() == 0) {
            return;
        }
        conta.debitarTarifa(taxa, TipoTransacao.TAXA_MANUTENCAO);
        persistirMovimentacao(conta, new Transacao(TipoTransacao.TAXA_MANUTENCAO, taxa));
    }

    @Transactional
    public void transferir(String cpfOrigem, String cpfDestino, BigDecimal valor) {
        ContaBancaria origem = buscarConta(cpfOrigem);
        ContaBancaria destino = buscarConta(cpfDestino);

        if (origem.getCpf().equals(destino.getCpf())) {
            throw new IllegalArgumentException("A conta de origem e destino devem ser diferentes.");
        }

        origem.debitarTransferencia(valor);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        contaDAO.transferirComTransacao(origem, destino, valor);
    }

    @Transactional
    public void removerConta(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        if (conta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new ContaComSaldoException("Não é possível remover conta com saldo positivo.");
        }
        contaDAO.deletar(conta.getCpf());
    }

    @Transactional(readOnly = true)
    public List<ContaBancaria> listarTodasOrdenadasPorSaldo() {
        return contaDAO.buscarTodas().stream()
                .sorted(Comparator.comparing(ContaBancaria::getSaldo).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContaBancaria buscarConta(String cpf) {
        String cpfNormalizado = validarCpf(cpf);
        return contaDAO.buscarPorCpf(cpfNormalizado)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada para o CPF: " + cpfNormalizado));
    }

    private void persistirMovimentacao(ContaBancaria conta, Transacao transacao) {
        contaDAO.movimentarComTransacao(conta, transacao);
    }

    private String validarCpf(String cpf) {
        String normalizado = Validador.normalizarCpf(cpf);
        if (!Validador.validarCpf(normalizado)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        return normalizado;
    }
}