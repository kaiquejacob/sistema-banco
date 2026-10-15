package br.com.sistemabanco.infrastructure.persistence.repository;

import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaJpaRepository
        extends JpaRepository<ContaEntity, String> {
}
