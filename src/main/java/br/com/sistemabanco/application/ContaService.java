package br.com.sistemabanco.application;

import br.com.sistemabanco.domain.exception.ContaComSaldoException;
import br.com.sistemabanco.domain.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.factory.ContaFactory;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.service.CepService;
import br.com.sistemabanco.infrastructure.persistence.ContaDAOImpl;
import br.com.sistemabanco.infrastructure.client.CepServiceImpl;
import br.com.sistemabanco.util.Validador;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ContaService {

    private final ContaDAO contaDAO;
    private final CepService cepService;

    public ContaService() {
        this.contaDAO = new ContaDAOImpl();
        this.cepService = new CepServiceImpl();
    }

    public ContaService(ContaDAO contaDAO, CepService cepService) {
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
        Transacao transacao = new Transacao(TipoTransacao.SAQUE, valor);
        persistirMovimentacao(conta, transacao, saldoAnterior);
    }

    public void transferir(String cpfOrigem, String cpfDestino, BigDecimal valor) {
        ContaBancaria origem = buscarConta(cpfOrigem);
        ContaBancaria destino = buscarConta(cpfDestino);

        if (origem.getCpf().equals(destino.getCpf())) {
            throw new IllegalArgumentException("A conta de origem e destino devem ser diferentes.");
        }

        BigDecimal saldoOrigemAnterior = origem.getSaldo();
        BigDecimal saldoDestinoAnterior = destino.getSaldo();
        origem.sacar(valor, TipoTransacao.TRANSFERENCIA_ENVIADA);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        if (contaDAO.suportaTransacoesAtomicas()) {
            contaDAO.transferirComTransacao(origem, destino, valor, saldoOrigemAnterior, saldoDestinoAnterior);
        } else {
            contaDAO.transferirComTransacao(origem, destino, valor);
        }
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
        if (contaDAO.suportaTransacoesAtomicas()) {
            contaDAO.movimentarComTransacao(conta, transacao, saldoAnterior);
        } else {
            contaDAO.atualizarSaldoESaques(conta);
            contaDAO.registrarTransacao(conta.getCpf(), transacao);
        }
    }

    private String validarCpf(String cpf) {
        String normalizado = Validador.normalizarCpf(cpf);
        if (!Validador.validarCpf(normalizado)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        return normalizado;
    }
}