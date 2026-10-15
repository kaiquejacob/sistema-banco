package br.com.sistemabanco.infrastructure.persistence.repository;

import br.com.sistemabanco.infrastructure.persistence.entity.TransacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoJpaRepository
        extends JpaRepository<TransacaoEntity, Integer> {

    List<TransacaoEntity> findByConta_CpfOrderByDataHoraAscIdAsc(
            String cpf
    );

    boolean existsByConta_Cpf(String cpf);
}
