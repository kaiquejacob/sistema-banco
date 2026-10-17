package br.com.sistemabanco.infrastructure.persistence.repository;

import br.com.sistemabanco.domain.model.ResumoConta;
import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContaJpaRepository
        extends JpaRepository<ContaEntity, String> {

    @Query("select new br.com.sistemabanco.domain.model.ResumoConta("
            + "c.cpf, c.titular, c.tipo, c.saldo) "
            + "from ContaEntity c order by c.saldo desc, c.cpf asc")
    List<ResumoConta> listarResumosPorSaldo(Pageable pageable);
}
