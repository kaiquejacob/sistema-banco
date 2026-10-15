package br.com.sistemabanco.infrastructure.persistence.mapper;

import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.model.ContaBancaria;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.ContaInvestimento;
import br.com.sistemabanco.domain.model.ContaPoupanca;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;

public final class ContaPersistenceMapper {

    private ContaPersistenceMapper() {

    }

    public static void atualizarEntity(
            ContaBancaria conta,
            ContaEntity entity
    ) {
        entity.setCpf(conta.getCpf());
        entity.setTitular(conta.getTitular());
        entity.setEmail(conta.getEmail());
        entity.setSaldo(conta.getSaldo());
        entity.setTipo(conta.getTipoConta());
        entity.setDataAbertura(conta.getDataAbertura());

        if (conta instanceof ContaCorrente contaCorrente) {
            entity.setSaquesRealizados(
                    contaCorrente.getSaquesRealizados()
            );
            entity.setDataUltimoSaque(
                    contaCorrente.getDataUltimoSaque()
            );

        }
    }

    public static ContaBancaria toDomain(ContaEntity entity)
            throws CpfInvalidoException, EmailInvalidoException {

        Endereco endereco =
                EnderecoPersistenceMapper.toDomain(
                        entity.getEndereco()
                );

        return switch (entity.getTipo()) {
            case CORRENTE -> new ContaCorrente(
                    entity.getTitular(),
                    entity.getSaldo(),
                    entity.getCpf(),
                    entity.getEmail(),
                    entity.getDataAbertura(),
                    endereco,
                    entity.getSaquesRealizados(),
                    entity.getDataUltimoSaque()
            );

            case POUPANCA -> new ContaPoupanca(
                    entity.getTitular(),
                    entity.getSaldo(),
                    entity.getCpf(),
                    entity.getEmail(),
                    entity.getDataAbertura(),
                    endereco
            );

            case INVESTIMENTO -> new ContaInvestimento(
                    entity.getTitular(),
                    entity.getSaldo(),
                    entity.getCpf(),
                    entity.getEmail(),
                    entity.getDataAbertura(),
                    endereco
            );
        };
    }
}
