package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.exception.*;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import br.com.sistemabanco.infrastructure.persistence.entity.EnderecoEntity;
import br.com.sistemabanco.infrastructure.persistence.entity.TransacaoEntity;
import br.com.sistemabanco.infrastructure.persistence.mapper.ContaPersistenceMapper;
import br.com.sistemabanco.infrastructure.persistence.mapper.EnderecoPersistenceMapper;
import br.com.sistemabanco.infrastructure.persistence.mapper.TransacaoPersistenceMapper;
import br.com.sistemabanco.infrastructure.persistence.repository.ContaJpaRepository;
import br.com.sistemabanco.infrastructure.persistence.repository.EnderecoJpaRepository;
import br.com.sistemabanco.infrastructure.persistence.repository.TransacaoJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ContaDAOJpa implements ContaDAO {

    private final ContaJpaRepository contaRepository;
    private final EnderecoJpaRepository enderecoRepository;
    private final TransacaoJpaRepository transacaoRepository;
    private final TransactionTemplate consultaEmNovaTransacao;

    public ContaDAOJpa(
            ContaJpaRepository contaRepository,
            EnderecoJpaRepository enderecoRepository,
            TransacaoJpaRepository transacaoRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.contaRepository = contaRepository;
        this.enderecoRepository = enderecoRepository;
        this.transacaoRepository = transacaoRepository;

        this.consultaEmNovaTransacao = new TransactionTemplate(transactionManager);
        this.consultaEmNovaTransacao.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        this.consultaEmNovaTransacao.setReadOnly(true);
    }

    @Override
    @Transactional
    public void salvar(ContaBancaria conta) {
        if (contaRepository.existsById(conta.getCpf())) {
            throw cpfJaCadastrado(conta.getCpf());
        }

        ContaEntity entity = new ContaEntity();
        ContaPersistenceMapper.atualizarEntity(conta, entity);

        try {
            entity = contaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (existeEmNovaTransacao(conta.getCpf())) {
                throw cpfJaCadastrado(conta.getCpf());
            }
            throw exception;
        }

        sincronizarEndereco(conta.getEndereco(), entity);

        for (Transacao transacao : conta.getHistorico()) {
            transacaoRepository.save(
                    TransacaoPersistenceMapper.toEntity(transacao, entity)
            );
        }
    }

    @Override
    @Transactional
    public void movimentarComTransacao(
            ContaBancaria conta,
            List<Transacao> novasTransacoes
    ) {
        ContaEntity entity = buscarEntity(conta.getCpf());

        atualizarEstadoFinanceiro(conta, entity);

        for (Transacao transacao : novasTransacoes) {
            transacaoRepository.save(
                    TransacaoPersistenceMapper.toEntity(transacao, entity)
            );
        }

        enviarAlteracoesTraduzindoConflito();
    }

    @Override
    @Transactional
    public void transferirComTransacao(
            ContaBancaria origem,
            ContaBancaria destino,
            BigDecimal valor
    ) {
        ContaEntity entityOrigem = buscarEntity(origem.getCpf());
        ContaEntity entityDestino = buscarEntity(destino.getCpf());

        atualizarEstadoFinanceiro(origem, entityOrigem);
        atualizarEstadoFinanceiro(destino, entityDestino);

        Transacao transacaoOrigem = ultimaTransacao(
                origem,
                TipoTransacao.TRANSFERENCIA_ENVIADA,
                valor
        );

        Transacao transacaoDestino = ultimaTransacao(
                destino,
                TipoTransacao.TRANSFERENCIA_RECEBIDA,
                valor
        );

        transacaoRepository.save(
                TransacaoPersistenceMapper.toEntity(
                        transacaoOrigem,
                        entityOrigem
                )
        );

        transacaoRepository.save(
                TransacaoPersistenceMapper.toEntity(
                        transacaoDestino,
                        entityDestino
                )
        );

        enviarAlteracoesTraduzindoConflito();
    }

    @Override
    @Transactional
    public void deletar(String cpf) {
        if (transacaoRepository.existsByConta_Cpf(cpf)) {
            throw new ContaComHistoricoException(
                    "Não é possível excluir uma conta que possui transações."
            );
        }

        contaRepository.findById(cpf).ifPresent(conta -> {
            enderecoRepository.findByConta_Cpf(cpf)
                    .ifPresent(enderecoRepository::delete);
            contaRepository.delete(conta);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumoConta> listarResumosPorSaldo(int pagina, int tamanho) {
        return contaRepository.listarResumosPorSaldo(
                PageRequest.of(pagina, tamanho)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long contarContas() {
        return contaRepository.count();
    }

    private boolean existeEmNovaTransacao(String cpf) {
        return Boolean.TRUE.equals(
                consultaEmNovaTransacao.execute(
                        status -> contaRepository.existsById(cpf)
                )
        );
    }

    private CpfJaCadastradoException cpfJaCadastrado(String cpf) {
        return new CpfJaCadastradoException(
                "Já existe uma conta cadastrada para o CPF: " + cpf
        );
    }

    private void enviarAlteracoesTraduzindoConflito() {
        try {
            contaRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw conflitoConcorrencia(exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContaBancaria> buscarPorCpf(String cpf) {
        return contaRepository.findById(cpf)
                .map(entity -> {
                    ContaBancaria conta = converterParaDominio(entity);

                    List<TransacaoEntity> transacoes =
                            transacaoRepository
                                    .findByConta_CpfOrderByDataHoraAscIdAsc(
                                            entity.getCpf()
                                    );

                    for (TransacaoEntity transacao : transacoes) {
                        conta.adicionarTransacaoExistente(
                                TransacaoPersistenceMapper.toDomain(transacao)
                        );
                    }

                    return conta;
                });
    }

    private ContaEntity buscarEntity(String cpf) {
        return contaRepository.findById(cpf)
                .orElseThrow(() -> new ContaNaoEncontradaException(
                        "Conta não encontrada para o CPF: " + cpf
                ));
    }

    private ContaBancaria converterParaDominio(ContaEntity entity) {
        try {
            return ContaPersistenceMapper.toDomain(entity);
        } catch (CpfInvalidoException | EmailInvalidoException exception) {
            throw new IllegalStateException(
                    "Os dados da conta armazenados no banco são inválidos.",
                    exception
            );
        }
    }

    private void atualizarEstadoFinanceiro(
            ContaBancaria conta,
            ContaEntity entity
    ) {
        entity.setSaldo(conta.getSaldo());

        if (conta instanceof ContaCorrente corrente) {
            entity.setSaquesRealizados(corrente.getSaquesRealizados());
            entity.setDataUltimoSaque(corrente.getDataUltimoSaque());
        }
    }

    private ConflitoConcorrenciaException conflitoConcorrencia(
            OptimisticLockingFailureException exception
    ) {
        return new ConflitoConcorrenciaException(
                "A conta foi alterada por outra operação. "
                        + "Consulte o saldo atualizado e tente novamente.",
                exception
        );
    }

    private void sincronizarEndereco(
            Endereco endereco,
            ContaEntity conta
    ) {
        Optional<EnderecoEntity> enderecoExistente =
                enderecoRepository.findByConta_Cpf(conta.getCpf());

        if (endereco == null) {
            enderecoExistente.ifPresent(enderecoRepository::delete);
            conta.setEndereco(null);
            return;
        }

        EnderecoEntity entity = enderecoExistente.orElseGet(
                () -> EnderecoPersistenceMapper.toEntity(
                        endereco,
                        conta
                )
        );

        entity.setConta(conta);
        entity.setCep(endereco.cep());
        entity.setLogradouro(endereco.logradouro());
        entity.setBairro(endereco.bairro());
        entity.setCidade(endereco.cidade());
        entity.setUf(endereco.estado());

        EnderecoEntity enderecoSalvo = enderecoRepository.save(entity);
        conta.setEndereco(enderecoSalvo);
    }

    private Transacao ultimaTransacao(
            ContaBancaria conta,
            TipoTransacao tipoEsperado,
            BigDecimal valor
    ) {
        List<Transacao> historico = conta.getHistorico();

        if (!historico.isEmpty()) {
            Transacao ultima = historico.get(historico.size() - 1);

            if (ultima.getTipo() == tipoEsperado
                    && ultima.getValor().compareTo(valor) == 0) {
                return ultima;
            }
        }

        return new Transacao(tipoEsperado, valor);
    }
}
