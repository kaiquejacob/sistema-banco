package br.com.sistemabanco.service;

import br.com.sistemabanco.exception.ContaComSaldoException;
import br.com.sistemabanco.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.factory.ContaFactory;
import br.com.sistemabanco.model.*;
import br.com.sistemabanco.repository.ContaDAO;
import br.com.sistemabanco.repository.impl.ContaDAOImpl;
import br.com.sistemabanco.service.impl.CepServiceImpl;

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
        Endereco endereco = cepService.buscarEnderecoPorCep(cep);
        ContaBancaria conta = ContaFactory.criarConta(tipo, titular, saldo, cpf, email, endereco);
        contaDAO.salvar(conta);
    }

    public void depositar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        conta.depositar(valor);
        contaDAO.atualizarSaldoESaques(conta);
        contaDAO.registrarTransacao(cpf, new Transacao(TipoTransacao.DEPOSITO, valor));

    }

    public void sacar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        conta.sacar(valor);
        contaDAO.atualizarSaldoESaques(conta);
        contaDAO.registrarTransacao(cpf, new Transacao(TipoTransacao.SAQUE, valor));
    }

    public void transferir(String cpfOrigem, String cpfDestino, BigDecimal valor) {
        ContaBancaria origem = buscarConta(cpfOrigem);
        ContaBancaria destino = buscarConta(cpfDestino);

        origem.sacar(valor, TipoTransacao.TRANSFERENCIA_ENVIADA);
        destino.depositar(valor, TipoTransacao.TRANSFERENCIA_RECEBIDA);

        contaDAO.transferirComTransacao(origem, destino, valor);
    }

    public void removerConta(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        if (conta.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            throw new ContaComSaldoException("Não é possível remover conta com saldo positivo.");
        }
        contaDAO.deletar(cpf);
    }

    public List<ContaBancaria> listarTodasOrdenadasPorSaldo() {
        return contaDAO.buscarTodas().stream()
                .sorted(Comparator.comparing(ContaBancaria::getSaldo).reversed())
                .collect(Collectors.toList());
    }

    public ContaBancaria buscarConta(String cpf) {
        return contaDAO.buscarPorCpf(cpf)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada para o CPF: " + cpf));
    }
}