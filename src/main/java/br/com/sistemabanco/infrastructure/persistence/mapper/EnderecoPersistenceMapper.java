package br.com.sistemabanco.infrastructure.persistence.mapper;

import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import br.com.sistemabanco.infrastructure.persistence.entity.EnderecoEntity;

public final class EnderecoPersistenceMapper {

    private EnderecoPersistenceMapper() {

    }

    public static EnderecoEntity toEntity(
            Endereco endereco,
            ContaEntity conta
    ) {
        if (endereco == null) {
            return null;
        }

        EnderecoEntity entity = new EnderecoEntity();

        entity.setConta(conta);
        entity.setCep(endereco.cep());
        entity.setLogradouro(endereco.logradouro());
        entity.setBairro(endereco.bairro());
        entity.setCidade(endereco.cidade());
        entity.setUf(endereco.estado());

        return entity;
    }

    public static Endereco toDomain(EnderecoEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Endereco(
                entity.getCep(),
                entity.getLogradouro(),
                entity.getBairro(),
                entity.getCidade(),
                entity.getUf()
        );
    }
}
