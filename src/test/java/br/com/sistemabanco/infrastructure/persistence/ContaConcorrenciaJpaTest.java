
package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.infrastructure.persistence.entity.ContaEntity;
import br.com.sistemabanco.infrastructure.persistence.repository.ContaJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ContaConcorrenciaJpaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ContaJpaRepository contaRepository;

    @Test
    void deveDetectarAtualizacaoConcorrenteComVersion() {
        TransactionTemplate transacao = new TransactionTemplate(
                transactionManager
        );

        String cpf = "52998224725";

        // Cria a conta dentro de uma transação.
        transacao.executeWithoutResult(status -> {
            ContaEntity conta = new ContaEntity();
            conta.setCpf(cpf);
            conta.setTitular("Cliente Concorrencia");
            conta.setEmail("concorrencia@example.com");
            conta.setSaldo(new BigDecimal("1000.00"));
            conta.setTipo(
                    br.com.sistemabanco.domain.model.TipoConta.CORRENTE
            );
            conta.setDataAbertura(java.time.LocalDate.now());
            conta.setSaquesRealizados(0);

            contaRepository.saveAndFlush(conta);
        });

        EntityManager primeiro = entityManagerFactory.createEntityManager();
        EntityManager segundo = entityManagerFactory.createEntityManager();

        try {
            primeiro.getTransaction().begin();
            segundo.getTransaction().begin();

            ContaEntity contaPrimeiro = primeiro.find(
                    ContaEntity.class, cpf
            );
            ContaEntity contaSegundo = segundo.find(
                    ContaEntity.class, cpf
            );

            assertNotNull(contaPrimeiro);
            assertNotNull(contaSegundo);
            assertEquals(
                    contaPrimeiro.getVersion(),
                    contaSegundo.getVersion()
            );

            // A primeira operação altera e confirma o saldo.
            contaPrimeiro.setSaldo(new BigDecimal("1100.00"));
            primeiro.getTransaction().commit();

            // A segunda tenta gravar usando a versão antiga.
            contaSegundo.setSaldo(new BigDecimal("900.00"));

            assertThrows(
                    OptimisticLockException.class,
                    segundo::flush
            );

        } finally {
            if (primeiro.getTransaction().isActive()) {
                primeiro.getTransaction().rollback();
            }

            if (segundo.getTransaction().isActive()) {
                segundo.getTransaction().rollback();
            }

            primeiro.close();
            segundo.close();
        }

        // Confirma que o saldo da primeira atualização foi preservado.
        transacao.executeWithoutResult(status -> {
            ContaEntity contaFinal = contaRepository.findById(cpf)
                    .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("1100.00")
                            .compareTo(contaFinal.getSaldo())
            );
        });

        // Remove o registro criado pelo teste.
        transacao.executeWithoutResult(status ->
                contaRepository.deleteById(cpf)
        );
    }
}
