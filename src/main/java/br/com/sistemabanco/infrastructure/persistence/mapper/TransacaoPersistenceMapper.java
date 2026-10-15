package br.com.sistemabanco.infrastructure.persistence.mapper;

import br.com.sistemabanco.domain.model.Transacao;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import br.com.sistemabanco.infrastructure.persistence.entity.TransacaoEntity;

public final class TransacaoPersistenceMapper {

    private TransacaoPersistenceMapper() {

    }

    public static TransacaoEntity toEntity(
            Transacao transacao,
            ContaEntity conta
    ) {
        if (transacao == null) {
            return null;
        }

        TransacaoEntity entity = new TransacaoEntity();

        entity.setConta(conta);
        entity.setTipo(transacao.getTipo());
        entity.setValor(transacao.getValor());
        entity.setDataHora(transacao.getDataHora());

        return entity;
    }

    public static Transacao toDomain(TransacaoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Transacao(
                entity.getTipo(),
                entity.getValor(),
                entity.getDataHora()
        );
    }
}
