package br.com.sistemabanco.integration;

import br.com.sistemabanco.application.ContaService;
import br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException;
import br.com.sistemabanco.domain.model.ContaCorrente;
import br.com.sistemabanco.domain.model.Endereco;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.domain.repository.CredencialDAO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConcorrenciaIntegracaoTest {

    private static final String CPF = "52998224725";
    private static final String SENHA = "SenhaTeste#1";

    @Autowired
    private ContaService contaService;

    @Autowired
    private ContaDAO contaDAO;

    @Autowired
    private CredencialDAO credencialDAO;

    @BeforeAll
    void prepararConta() {
        Endereco endereco = new Endereco("01001000", "Praca da Se", "Se", "Sao Paulo", "SP");

        contaDAO.salvar(
                new ContaCorrente("Cliente Concorrente", new BigDecimal("1000.00"), CPF, "concorrente@example.com", endereco)
        );

        credencialDAO.criarCliente(CPF, SENHA);
    }

    @Test
    void doisSaquesConcorrentesNaMesmaContaDevemGerarUmConflito() throws Exception {
        int numeroDeSaques = 2;
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fim = new CountDownLatch(numeroDeSaques);

        try (ExecutorService executor = Executors.newFixedThreadPool(numeroDeSaques)) {
            List<Future<?>> resultados = new java.util.ArrayList<>();

            for (int i = 0; i < numeroDeSaques; i++) {
                resultados.add(executor.submit(() -> {
                    try {
                        inicio.await();
                        contaService.sacar(CPF, new BigDecimal("100.00"));
                        return null;
                    } catch (Exception exception) {
                        return exception;
                    } finally {
                        fim.countDown();
                    }
                }));
            }

            inicio.countDown();
            fim.await();

            long sucessos = resultados.stream()
                    .filter(resultado -> {
                        try {
                            return resultado.get() == null;
                        } catch (Exception exception) {
                            return false;
                        }
                    })
                    .count();

            long conflitos = resultados.stream()
                    .filter(resultado -> {
                        try {
                            return resultado.get() instanceof ConflitoConcorrenciaException;
                        } catch (Exception exception) {
                            return false;
                        }
                    })
                    .count();

            assertEquals(1, sucessos);
            assertEquals(1, conflitos);

            BigDecimal saldoFinal = contaService.buscarConta(CPF).getSaldo();
            assertEquals(new BigDecimal("900.00"), saldoFinal);
        }
    }
}