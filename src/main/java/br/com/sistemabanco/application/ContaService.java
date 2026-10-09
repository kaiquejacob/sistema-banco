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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public void depositar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        BigDecimal saldoAnterior = conta.getSaldo();
        conta.depositar(valor);
        Transacao transacao = new Transacao(TipoTransacao.DEPOSITO, valor);
        persistirMovimentacao(conta, transacao, saldoAnterior);

    }

    public void sacar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        BigDecimal saldoAnterior = conta.getSaldo();
        conta.sacar(valor);
        Transacao transacao = new Transacao(TipoTransacao.SAQUE, saldoAnterior.subtract(conta.getSaldo()));
        persistirMovimentacao(conta, transacao, saldoAnterior);
    }

    public void cobrarTaxaManutencao(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        BigDecimal saldoAnterior = conta.getSaldo();
        BigDecimal taxa = conta.getTipoConta().calcularTaxaManutencao();
        if (taxa.signum() == 0) {
            return;
        }
        conta.debitarTarifa(taxa, TipoTransacao.TAXA_MANUTENCAO);
        persistirMovimentacao(conta, new Transacao(TipoTransacao.TAXA_MANUTENCAO, taxa), saldoAnterior);
    }

    public void transferir(String cpfOrigem, String cpfDestino, BigDecimal valor) {
        ContaBancaria origem = buscarConta(cpfOrigem);
        ContaBancaria destino = buscarConta(cpfDestino);

        if (origem.getCpf().equals(destino.getCpf())) {
            throw new IllegalArgumentException("A conta de origem e destino devem ser diferentes.");
        }

        BigDecimal saldoOrigemAnterior = origem.getSaldo();
        BigDecimal saldoDestinoAnterior = destino.getSaldo();
        origem.debitarTransferencia(valor);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        contaDAO.transferirComTransacao(origem, destino, valor, saldoOrigemAnterior, saldoDestinoAnterior);
    }

    public void removerConta(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        if (conta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new ContaComSaldoException("Não é possível remover conta com saldo positivo.");
        }
        contaDAO.deletar(conta.getCpf());
    }

    public List<ContaBancaria> listarTodasOrdenadasPorSaldo() {
        return contaDAO.buscarTodas().stream()
                .sorted(Comparator.comparing(ContaBancaria::getSaldo).reversed())
                .collect(Collectors.toList());
    }

    public ContaBancaria buscarConta(String cpf) {
        String cpfNormalizado = validarCpf(cpf);
        return contaDAO.buscarPorCpf(cpfNormalizado)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada para o CPF: " + cpfNormalizado));
    }

    private void persistirMovimentacao(ContaBancaria conta, Transacao transacao, BigDecimal saldoAnterior) {
        /*
         * O DAO JDBC garante atomicidade de saldo e histórico. O caminho
         * legado é mantido para DAOs de teste e implementações externas.
         */
        contaDAO.movimentarComTransacao(conta, transacao, saldoAnterior);
    }

    private String validarCpf(String cpf) {
        String normalizado = Validador.normalizarCpf(cpf);
        if (!Validador.validarCpf(normalizado)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        return normalizado;
    }
}