package br.com.sistemabanco.infrastructure.persistence.repository;

import br.com.sistemabanco.infrastructure.persistence.entity.EnderecoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnderecoJpaRepository
        extends JpaRepository<EnderecoEntity, Integer> {

    Optional<EnderecoEntity> findByConta_Cpf(String cpf);
}
