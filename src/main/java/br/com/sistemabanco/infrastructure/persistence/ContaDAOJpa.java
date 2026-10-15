package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.model.TipoTransacao;
import br.com.sistemabanco.domain.model.Transacao;
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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ContaDAOJpa implements ContaDAO {

    private final ContaJpaRepository contaRepository;
    private final EnderecoJpaRepository enderecoRepository;
    private final TransacaoJpaRepository transacaoRepository;

    public ContaDAOJpa(
            ContaJpaRepository contaRepository,
            EnderecoJpaRepository enderecoRepository,
            TransacaoJpaRepository transacaoRepository
    ) {
        this.contaRepository = contaRepository;
        this.enderecoRepository = enderecoRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Override
    @Transactional
    public void salvar(ContaBancaria conta) {
        if (contaRepository.existsById(conta.getCpf())) {
            throw new CpfJaCadastradoException(
                    "Já existe uma conta cadastrada para o CPF: "
                            + conta.getCpf()
            );
        }

        ContaEntity entity = new ContaEntity();
        ContaPersistenceMapper.atualizarEntity(conta, entity);

        try {
            entity = contaRepository.saveAndFlush(entity);

            sincronizarEndereco(conta.getEndereco(), entity);

            for (Transacao transacao : conta.getHistorico()) {
                transacaoRepository.save(
                        TransacaoPersistenceMapper.toEntity(
                                transacao,
                                entity
                        )
                );
            }
        } catch (DataIntegrityViolationException exception) {
            if (contaRepository.existsById(conta.getCpf())) {
                throw new CpfJaCadastradoException(
                        "Já existe uma conta cadastrada para o CPF: "
                                + conta.getCpf()
                );
            }

            throw exception;
        }
    }

    @Override
    @Transactional
    public void atualizarSaldoESaques(ContaBancaria conta) {
        ContaEntity entity = buscarEntity(conta.getCpf());

        atualizarEstadoFinanceiro(conta, entity);
    }

    @Override
    @Transactional
    public void registrarTransacao(String cpf, Transacao transacao) {
        ContaEntity conta = buscarEntity(cpf);

        transacaoRepository.save(
                TransacaoPersistenceMapper.toEntity(transacao, conta)
        );
    }

    @Override
    @Transactional
    public void movimentarComTransacao(
            ContaBancaria conta,
            Transacao transacao
    ) {
        movimentarComTransacao(conta, transacao, null);
    }

    @Override
    @Transactional
    public void movimentarComTransacao(
            ContaBancaria conta,
            Transacao transacao,
            BigDecimal saldoAnterior
    ) {
        ContaEntity entity = buscarEntity(conta.getCpf());

        verificarSaldoAnterior(entity, saldoAnterior);

        atualizarEstadoFinanceiro(conta, entity);

        transacaoRepository.save(
                TransacaoPersistenceMapper.toEntity(transacao, entity)
        );
    }

    @Override
    @Transactional
    public void transferirComTransacao(
            ContaBancaria origem,
            ContaBancaria destino,
            BigDecimal valor
    ) {
        transferirComTransacao(
                origem,
                destino,
                valor,
                null,
                null
        );
    }

    @Override
    @Transactional
    public void transferirComTransacao(
            ContaBancaria origem,
            ContaBancaria destino,
            BigDecimal valor,
            BigDecimal saldoOrigemAnterior,
            BigDecimal saldoDestinoAnterior
    ) {
        ContaEntity entityOrigem = buscarEntity(origem.getCpf());
        ContaEntity entityDestino = buscarEntity(destino.getCpf());

        verificarSaldoAnterior(entityOrigem, saldoOrigemAnterior);
        verificarSaldoAnterior(entityDestino, saldoDestinoAnterior);

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
    }

    @Override
    @Transactional
    public void deletar(String cpf) {
        if (transacaoRepository.existsByConta_Cpf(cpf)) {
            throw new IllegalStateException(
                    "Não é possível excluir uma conta que possui transações."
            );
        }

        contaRepository.findById(cpf).ifPresent(contaRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContaBancaria> buscarTodas() {
        List<ContaEntity> entities = contaRepository.findAll();
        List<ContaBancaria> contas = new ArrayList<>();

        for (ContaEntity entity : entities) {
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

            contas.add(conta);
        }

        return contas;
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
                .orElseThrow(() -> new IllegalArgumentException(
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

    private void verificarSaldoAnterior(
            ContaEntity entity,
            BigDecimal saldoAnterior
    ) {
        if (saldoAnterior != null
                && entity.getSaldo().compareTo(saldoAnterior) != 0) {
            throw new ConflitoConcorrenciaException(
                    "A conta foi alterada por outra operação. "
                            + "Consulte o saldo atualizado e tente novamente."
            );
        }
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
