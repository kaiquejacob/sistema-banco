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
import java.util.List;

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
        conta.registrarAbertura();
        contaDAO.salvar(conta);
    }

    @Transactional
    public void depositar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        int tamanhoAntes = conta.getHistorico().size();

        conta.depositar(valor);

        persistirNovasTransacoes(conta, tamanhoAntes);
    }

    @Transactional
    public void sacar(String cpf, BigDecimal valor) {
        ContaBancaria conta = buscarConta(cpf);
        int tamanhoAntes = conta.getHistorico().size();

        conta.sacar(valor);

        persistirNovasTransacoes(conta, tamanhoAntes);
    }

    @Transactional
    public void cobrarTaxaManutencao(String cpf) {
        ContaBancaria conta = buscarConta(cpf);
        BigDecimal taxa = conta.getTipoConta().calcularTaxaManutencao();
        if (taxa.signum() == 0) {
            return;
        }
        int tamanhoAntes = conta.getHistorico().size();

        conta.debitarTarifa(taxa, TipoTransacao.TAXA_MANUTENCAO);

        persistirNovasTransacoes(conta, tamanhoAntes);
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

    static final int TAMANHO_MAXIMO_PAGINA = 100;   // coloque junto dos campos da classe

    @Transactional(readOnly = true)
    public PaginaDeContas listarPorSaldo(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA + ".");
        }

        List<ResumoConta> itens = contaDAO.listarResumosPorSaldo(pagina, tamanho);
        long total = contaDAO.contarContas();
        return new PaginaDeContas(itens, total, pagina, tamanho);
    }

    @Transactional(readOnly = true)
    public ContaBancaria buscarConta(String cpf) {
        String cpfNormalizado = validarCpf(cpf);
        return contaDAO.buscarPorCpf(cpfNormalizado)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada para o CPF: " + cpfNormalizado));
    }

    private void persistirNovasTransacoes(ContaBancaria conta, int tamanhoAntes) {
        List<Transacao> historico = conta.getHistorico();
        List<Transacao> novas = List.copyOf(historico.subList(tamanhoAntes, historico.size()));
        contaDAO.movimentarComTransacao(conta, novas);
    }

    private String validarCpf(String cpf) {
        String normalizado = Validador.normalizarCpf(cpf);
        if (!Validador.validarCpf(normalizado)) {
            throw new CpfInvalidoException("CPF inválido: " + cpf);
        }
        return normalizado;
    }
}